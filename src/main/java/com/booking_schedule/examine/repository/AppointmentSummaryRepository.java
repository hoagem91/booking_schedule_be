package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Appointment;
import com.booking_schedule.examine.entity.Doctor;
import com.booking_schedule.examine.enums.EAppointmentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Locale;

@Repository
@RequiredArgsConstructor
public class AppointmentSummaryRepository {
    private final EntityManager entityManager;

    public record DoctorGroup(Long doctorId, String doctorCode, String doctorName) {}
    public record StatusCount(Long doctorId, EAppointmentStatus status, Long count) {}

    public List<DoctorGroup> findDoctors(Long doctorId, String doctorKeyword) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(DoctorGroup.class);
        var doctor = query.from(Doctor.class);
        var name = doctor.get("user").get("fullName");
        query.select(cb.construct(DoctorGroup.class, doctor.get("id"), doctor.get("doctorCode"), name));
        var predicate = doctorMatches(doctor, cb, doctorKeyword);
        if (doctorId != null) predicate = cb.and(predicate, cb.equal(doctor.get("id"), doctorId));
        query.where(predicate);
        query.orderBy(cb.asc(name), cb.asc(doctor.get("id")));
        return entityManager.createQuery(query).getResultList();
    }

    public List<StatusCount> countByDoctorAndStatus(Specification<Appointment> specification, String doctorKeyword) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(StatusCount.class);
        var appointment = query.from(Appointment.class);
        var doctorId = appointment.get("doctor").get("id");
        var status = appointment.get("status");
        query.select(cb.construct(StatusCount.class, doctorId, status, cb.count(appointment)));
        query.where(cb.and(specification.toPredicate(appointment, query, cb),
                doctorMatches(appointment.get("doctor"), cb, doctorKeyword)));
        query.groupBy(doctorId, status);
        return entityManager.createQuery(query).getResultList();
    }

    private Predicate doctorMatches(Path<Doctor> doctor, CriteriaBuilder cb, String keyword) {
        if (keyword == null || keyword.isBlank()) return cb.conjunction();
        String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return cb.or(cb.like(cb.lower(doctor.get("doctorCode")), pattern, '\\'),
                cb.like(cb.lower(doctor.get("user").get("fullName")), pattern, '\\'));
    }
}
