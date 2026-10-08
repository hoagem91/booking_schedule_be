package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.response.TimeSlotResponse;
import com.booking_schedule.examine.entity.*;
import com.booking_schedule.examine.enums.*;
import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.mapper.TimeSlotMapper;
import com.booking_schedule.examine.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeSlotService {
    private final TimeSlotRepository slots;
    private final DoctorScheduleRepository schedules;
    private final AppointmentRepository appointments;
    private final TimeSlotMapper mapper;

    public List<TimeSlotResponse> getAllTimeSlot(Long scheduleId) {
        DoctorSchedule schedule = schedules.findById(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
        List<TimeSlot> all = slots.findByDoctorScheduleId(scheduleId);
        LocalDateTime now = LocalDateTime.now();
        return all.stream().filter(s -> TimeSlotPolicy.available(schedule, s, all, now))
                .sorted(java.util.Comparator.comparing(TimeSlot::getStartTime))
                .map(mapper::toTimeSlotResponse).toList();
    }

    @Transactional
    public TimeSlotResponse changeStatus(Long id, ETimeSlotStatus status) {
        if (status == null || status == ETimeSlotStatus.FULL) throw new AppException(ErrorCode.TIME_SLOT_INVALID_STATUS);
        LockedSlots locked = lock(id);
        TimeSlot slot = locked.slot();
        if (status == ETimeSlotStatus.CLOSED || status == ETimeSlotStatus.UNAVAILABLE) {
            if (slot.getBookedCount() > 0 || appointments.existsByTimeSlotIdAndStatusIn(id,
                    List.of(EAppointmentStatus.PENDING, EAppointmentStatus.CONFIRMED, EAppointmentStatus.IN_PROGRESS)))
                throw new AppException(ErrorCode.TIME_SLOT_HAS_BOOKINGS);
            slot.setManuallyClosed(true);
        } else {
            LocalDateTime now = LocalDateTime.now();
            if (!TimeSlotPolicy.acceptingSchedule(locked.schedule(), now)
                    || slot.getBookedCount() >= slot.getMaxPatients()
                    || TimeSlotPolicy.bookedCount(locked.all()) >= locked.schedule().getMaxPatients()
                    || !LocalDateTime.of(locked.schedule().getWorkDate(), slot.getStartTime()).isAfter(now))
                throw new AppException(ErrorCode.TIME_SLOT_NOT_AVAILABLE);
            slot.setManuallyClosed(false);
        }
        TimeSlotPolicy.synchronize(locked.schedule(), locked.all());
        slots.flush();
        return mapper.toTimeSlotResponse(slot);
    }

    /** Internal only: Appointment must persist in the SAME transaction and call this once. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserveSlot(Long id) {
        LockedSlots locked = lock(id);
        if (!TimeSlotPolicy.available(locked.schedule(), locked.slot(), locked.all(), LocalDateTime.now()))
            throw new AppException(ErrorCode.TIME_SLOT_NOT_AVAILABLE);
        locked.slot().setBookedCount(locked.slot().getBookedCount() + 1);
        TimeSlotPolicy.synchronize(locked.schedule(), locked.all());
    }

    /** Call once when an Appointment transitions from an active reservation to CANCELLED. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseSlot(Long id) {
        LockedSlots locked = lock(id);
        if (locked.slot().getBookedCount() <= 0) throw new AppException(ErrorCode.TIME_SLOT_INVALID_COUNT);
        locked.slot().setBookedCount(locked.slot().getBookedCount() - 1);
        TimeSlotPolicy.synchronize(locked.schedule(), locked.all());
    }

    private LockedSlots lock(Long id) {
        Long scheduleId = slots.findScheduleIdBySlotId(id).orElseThrow(() -> new AppException(ErrorCode.TIME_SLOT_NOT_FOUND));
        // Consistent order with schedule writes: parent first, then slots by ID.
        DoctorSchedule schedule = schedules.findLockedById(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
        List<TimeSlot> all = slots.findAllLockedByScheduleId(scheduleId);
        TimeSlot slot = all.stream().filter(s -> s.getId().equals(id)).findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.TIME_SLOT_NOT_FOUND));
        return new LockedSlots(schedule, slot, all);
    }

    private record LockedSlots(DoctorSchedule schedule, TimeSlot slot, List<TimeSlot> all) {}
}
