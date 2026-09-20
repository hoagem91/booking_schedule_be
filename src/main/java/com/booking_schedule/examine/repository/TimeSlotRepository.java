package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByDoctorScheduleId(Long scheduleId);
    // Tìm các ca trống (chưa kín chỗ)
    List<TimeSlot> findByDoctorScheduleIdAndStatusAndBookedCountLessThan(Long scheduleId, String status, Integer maxPatients);
}