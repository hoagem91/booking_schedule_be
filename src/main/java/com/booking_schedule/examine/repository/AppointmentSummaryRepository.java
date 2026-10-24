package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Appointment;
import com.booking_schedule.examine.entity.Doctor;
import com.booking_schedule.examine.enums.EAppointmentStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AppointmentSummaryRepository {
    private final EntityManager entityManager;

    public record DoctorGroup(Long doctorId, String doctorCode, String doctorName) {}
    public record StatusCount(Long doctorId, EAppointmentStatus status, Long count) {}

    public List<DoctorGroup> findDoctors(Long doctorId) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(DoctorGroup.class);
        var doctor = query.from(Doctor.class);
        var name = doctor.get("user").get("fullName");
        query.select(cb.construct(DoctorGroup.class, doctor.get("id"), doctor.get("doctorCode"), name));
        if (doctorId != null) query.where(cb.equal(doctor.get("id"), doctorId));
        query.orderBy(cb.asc(name), cb.asc(doctor.get("id")));
        return entityManager.createQuery(query).getResultList();
    }

    public List<StatusCount> countByDoctorAndStatus(Specification<Appointment> specification) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(StatusCount.class);
        var appointment = query.from(Appointment.class);
        var doctorId = appointment.get("doctor").get("id");
        var status = appointment.get("status");
        query.select(cb.construct(StatusCount.class, doctorId, status, cb.count(appointment)));
        query.where(specification.toPredicate(appointment, query, cb));
        query.groupBy(doctorId, status);
        return entityManager.createQuery(query).getResultList();
    }
}
