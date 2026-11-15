package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.dto.response.DashboardResponse.*;
import com.booking_schedule.examine.enums.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

/** Aggregation runs in the database; never fetch a page and treat it as a total. */
@Repository
@RequiredArgsConstructor
public class DashboardRepository {
    private final EntityManager em;

    public Map<EAppointmentStatus, Long> statusCounts(LocalDate from, LocalDate to) {
        Map<EAppointmentStatus, Long> counts = new EnumMap<>(EAppointmentStatus.class);
        for (var status : EAppointmentStatus.values()) counts.put(status, 0L);
        em.createQuery("select a.status, count(a) from Appointment a where a.appointmentDate between :from and :to group by a.status", Object[].class)
                .setParameter("from", from).setParameter("to", to).getResultList()
                .forEach(row -> counts.put((EAppointmentStatus) row[0], (Long) row[1]));
        return counts;
    }

    public List<DailyAppointments> dailyAppointments(LocalDate from, LocalDate to) {
        return em.createQuery("select a.appointmentDate, count(a) from Appointment a where a.appointmentDate between :from and :to group by a.appointmentDate order by a.appointmentDate", Object[].class)
                .setParameter("from", from).setParameter("to", to).getResultList().stream()
                .map(row -> new DailyAppointments((LocalDate) row[0], (Long) row[1])).toList();
    }

    public long newlyCreated(LocalDate from, LocalDate to) {
        return em.createQuery("select count(a) from Appointment a where a.createdAt >= :from and a.createdAt < :until", Long.class)
                .setParameter("from", from.atStartOfDay()).setParameter("until", to.plusDays(1).atStartOfDay()).getSingleResult();
    }

    public List<RankedItem> top(LocalDate from, LocalDate to, boolean doctors) {
        // Only these two constant paths are permitted; no client-supplied HQL.
        String path = doctors ? "a.doctor" : "a.service.specialty";
        String name = doctors ? "x.user.fullName" : "x.name";
        return em.createQuery("select x.id, " + name + ", count(a) from Appointment a join " + path
                        + " x where a.appointmentDate between :from and :to and a.status not in :excluded group by x.id, "
                        + name + " order by count(a) desc, x.id asc", Object[].class)
                .setParameter("from", from).setParameter("to", to)
                .setParameter("excluded", List.of(EAppointmentStatus.CANCELLED, EAppointmentStatus.NO_SHOW))
                .setMaxResults(5).getResultList().stream()
                .map(row -> new RankedItem((Long) row[0], (String) row[1], (Long) row[2])).toList();
    }

    public List<DailyRevenue> dailyRevenue(LocalDate from, LocalDate to) {
        return em.createQuery("select cast(p.paidAt as LocalDate), count(p), sum(p.amount) from Payment p where p.paymentStatus = :success and p.paidAt >= :from and p.paidAt < :until group by cast(p.paidAt as LocalDate) order by cast(p.paidAt as LocalDate)", Object[].class)
                .setParameter("success", EPaymentStatus.SUCCESS).setParameter("from", from.atStartOfDay())
                .setParameter("until", to.plusDays(1).atStartOfDay()).getResultList().stream()
                .map(row -> new DailyRevenue((LocalDate) row[0], (Long) row[1], (BigDecimal) row[2])).toList();
    }

    public List<MethodRevenue> byMethod(LocalDate from, LocalDate to) {
        return em.createQuery("select p.paymentMethod, count(p), sum(p.amount) from Payment p where p.paymentStatus = :success and p.paidAt >= :from and p.paidAt < :until group by p.paymentMethod order by p.paymentMethod", Object[].class)
                .setParameter("success", EPaymentStatus.SUCCESS).setParameter("from", from.atStartOfDay())
                .setParameter("until", to.plusDays(1).atStartOfDay()).getResultList().stream()
                .map(row -> new MethodRevenue((String) row[0], (Long) row[1], (BigDecimal) row[2])).toList();
    }

    public long missingPaidAt() {
        return em.createQuery("select count(p) from Payment p where p.paymentStatus = :success and p.paidAt is null", Long.class)
                .setParameter("success", EPaymentStatus.SUCCESS).getSingleResult();
    }

    public Summary summary(LocalDate today) {
        var counts = statusCounts(today, today);
        var collected = dailyRevenue(today, today).stream().map(DailyRevenue::collectedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pending = em.createQuery("select count(a) from Appointment a where a.status = :status", Long.class)
                .setParameter("status", EAppointmentStatus.PENDING).getSingleResult();
        long review = em.createQuery("select count(t) from PaymentTransaction t where t.status = :status", Long.class)
                .setParameter("status", EPaymentTransactionStatus.REVIEW_REQUIRED).getSingleResult();
        long patients = em.createQuery("select count(p) from Patient p", Long.class).getSingleResult();
        long doctors = em.createQuery("select count(distinct s.doctor.id) from DoctorSchedule s where s.workDate = :today and s.status in :statuses and s.doctor.status = :active", Long.class)
                .setParameter("today", today).setParameter("statuses", List.of(EStatusDoctorSchedule.OPEN, EStatusDoctorSchedule.FULL))
                .setParameter("active", EStatus.ACTIVE).getSingleResult();
        long specialties = em.createQuery("select count(s) from Specialty s where s.status = :active", Long.class)
                .setParameter("active", EStatus.ACTIVE).getSingleResult();
        long services = em.createQuery("select count(s) from Service s where s.status = :active", Long.class)
                .setParameter("active", EStatus.ACTIVE).getSingleResult();
        return new Summary(today, "Asia/Ho_Chi_Minh", counts.values().stream().mapToLong(Long::longValue).sum(),
                counts.get(EAppointmentStatus.CONFIRMED), counts.get(EAppointmentStatus.COMPLETED), collected,
                pending, review, patients, doctors, specialties, services, missingPaidAt());
    }

    public Page<TodayAppointment> todayAppointments(LocalDateTime now, boolean upcomingOnly, Pageable page) {
        String where = " where a.appointmentDate = :today" + (upcomingOnly
                ? " and (a.status = :inProgress or (a.status = :confirmed and a.timeSlot.startTime >= :now))" : "");
        var query = em.createQuery("select new com.booking_schedule.examine.dto.response.DashboardResponse$TodayAppointment(a.id, a.appointmentCode, a.patient.id, a.patient.user.fullName, a.doctor.id, a.doctor.user.fullName, s.id, s.name, a.service.name, a.appointmentDate, a.timeSlot.startTime, a.timeSlot.endTime, a.status) from Appointment a left join a.service.specialty s"
                + where + " order by a.timeSlot.startTime asc, a.id asc", TodayAppointment.class);
        var count = em.createQuery("select count(a) from Appointment a" + where, Long.class);
        for (var q : List.of(query, count)) {
            q.setParameter("today", now.toLocalDate());
            if (upcomingOnly) {
                q.setParameter("inProgress", EAppointmentStatus.IN_PROGRESS);
                q.setParameter("confirmed", EAppointmentStatus.CONFIRMED);
                q.setParameter("now", now.toLocalTime());
            }
        }
        return new PageImpl<>(query.setFirstResult(Math.toIntExact(page.getOffset())).setMaxResults(page.getPageSize())
                .getResultList(), page, count.getSingleResult());
    }
}
