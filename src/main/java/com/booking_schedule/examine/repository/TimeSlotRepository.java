package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByDoctorScheduleId(Long scheduleId);
    @org.springframework.data.jpa.repository.Query("select t.doctorSchedule.id from TimeSlot t where t.id = :id")
    Optional<Long> findScheduleIdBySlotId(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from TimeSlot t where t.doctorSchedule.id = :scheduleId order by t.id")
    List<TimeSlot> findAllLockedByScheduleId(@org.springframework.data.repository.query.Param("scheduleId") Long scheduleId);
}
