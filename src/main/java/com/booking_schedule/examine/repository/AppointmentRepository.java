package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Appointment;
import com.booking_schedule.examine.enums.EAppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Appointment> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("select a.timeSlot.doctorSchedule.id from Appointment a where a.id = :id")
    Optional<Long> findScheduleIdByAppointmentId(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Appointment a where a.patient.id = :patientId and a.appointmentDate = :date and a.status in :statuses and a.timeSlot.startTime < :end and a.timeSlot.endTime > :start")
    List<Appointment> findOverlaps(@org.springframework.data.repository.query.Param("patientId") Long patientId,
            @org.springframework.data.repository.query.Param("date") LocalDate date,
            @org.springframework.data.repository.query.Param("statuses") java.util.Collection<EAppointmentStatus> statuses,
            @org.springframework.data.repository.query.Param("start") java.time.LocalTime start,
            @org.springframework.data.repository.query.Param("end") java.time.LocalTime end);
    boolean existsByTimeSlotIdAndStatusIn(Long slotId, java.util.Collection<EAppointmentStatus> statuses);
    boolean existsByTimeSlotDoctorScheduleId(Long scheduleId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    List<Appointment> findByTimeSlotDoctorScheduleIdAndStatusIn(Long scheduleId, java.util.Collection<EAppointmentStatus> statuses);
    Optional<Appointment> findByAppointmentCode(String appointmentCode);
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, LocalDate date);
    List<Appointment> findByStatus(EAppointmentStatus status);
    boolean existsByDoctorId(Long doctorId);
    boolean existsByServiceId(Long serviceId);
}
