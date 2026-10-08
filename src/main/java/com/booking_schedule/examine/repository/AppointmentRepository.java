package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.Appointment;
import com.booking_schedule.examine.enums.EAppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    boolean existsByTimeSlotIdAndStatusIn(Long slotId, java.util.Collection<EAppointmentStatus> statuses);
    boolean existsByTimeSlotDoctorScheduleId(Long scheduleId);
    List<Appointment> findByTimeSlotDoctorScheduleIdAndStatusIn(Long scheduleId, java.util.Collection<EAppointmentStatus> statuses);
    Optional<Appointment> findByAppointmentCode(String appointmentCode);
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, LocalDate date);
    List<Appointment> findByStatus(EAppointmentStatus status);
    boolean existsByDoctorId(Long doctorId);
    boolean existsByServiceId(Long serviceId);
}
