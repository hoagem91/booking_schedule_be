package com.booking_schedule.examine.service;

import com.booking_schedule.examine.entity.*;
import com.booking_schedule.examine.enums.*;
import java.time.LocalDateTime;
import java.util.List;

/** Shared rules for schedule cascades, individual closures and booking capacity. */
public final class TimeSlotPolicy {
    private TimeSlotPolicy() {}

    public static long bookedCount(List<TimeSlot> slots) {
        return slots.stream().mapToLong(TimeSlot::getBookedCount).sum();
    }

    public static boolean acceptingSchedule(DoctorSchedule schedule, LocalDateTime now) {
        return schedule.getStatus() == EStatusDoctorSchedule.OPEN
                && schedule.getDoctor().getStatus() == EStatus.ACTIVE
                && schedule.getDoctor().getUser() != null
                && "ACTIVE".equals(schedule.getDoctor().getUser().getStatus())
                && schedule.getDoctor().getSpecialty().getStatus() == EStatus.ACTIVE
                && !schedule.getWorkDate().isBefore(now.toLocalDate());
    }

    public static boolean available(DoctorSchedule schedule, TimeSlot slot, List<TimeSlot> slots, LocalDateTime now) {
        return acceptingSchedule(schedule, now)
                && !slot.isManuallyClosed() && slot.getStatus() == ETimeSlotStatus.AVAILABLE
                && slot.getBookedCount() < slot.getMaxPatients()
                && bookedCount(slots) < schedule.getMaxPatients()
                && LocalDateTime.of(schedule.getWorkDate(), slot.getStartTime()).isAfter(now);
    }

    public static void synchronize(DoctorSchedule schedule, List<TimeSlot> slots) {
        long total = bookedCount(slots);
        for (TimeSlot slot : slots) {
            if (schedule.getStatus() != EStatusDoctorSchedule.OPEN || slot.isManuallyClosed() || slot.getMaxPatients() == 0)
                slot.setStatus(ETimeSlotStatus.CLOSED);
            else if (slot.getBookedCount() >= slot.getMaxPatients()) slot.setStatus(ETimeSlotStatus.FULL);
            else slot.setStatus(total >= schedule.getMaxPatients() ? ETimeSlotStatus.CLOSED : ETimeSlotStatus.AVAILABLE);
        }
    }
}
