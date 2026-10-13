package com.booking_schedule.examine.service;

import com.booking_schedule.examine.entity.*;
import com.booking_schedule.examine.enums.EAppointmentStatus;
import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Caller must hold the schedule lock before changing an appointment. */
@Service
@RequiredArgsConstructor
public class AppointmentLifecycle {
    private final TimeSlotService timeSlots;
    private final PromotionRepository promotions;
    private final NotificationRepository notifications;

    @Transactional(propagation = Propagation.MANDATORY)
    public void cancel(Appointment appointment, String note, boolean tolerateLegacyCounter) {
        if (appointment.getStatus() == EAppointmentStatus.CANCELLED) return;
        if (appointment.getStatus() != EAppointmentStatus.PENDING && appointment.getStatus() != EAppointmentStatus.CONFIRMED)
            throw new AppException(ErrorCode.APPOINTMENT_INVALID_TRANSITION);
        if (!tolerateLegacyCounter || appointment.getTimeSlot().getBookedCount() > 0)
            timeSlots.releaseSlot(appointment.getTimeSlot().getId());
        if (appointment.getPromotion() != null) {
            Promotion promotion = promotions.findLockedById(appointment.getPromotion().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
            promotion.setUsedCount(Math.max(0, (promotion.getUsedCount() == null ? 0 : promotion.getUsedCount()) - 1));
        }
        appointment.setStatus(EAppointmentStatus.CANCELLED);
        if (note != null) appointment.setNote(note);
        notifyPatient(appointment, "Lịch hẹn đã bị hủy", "APPOINTMENT_CANCELLED");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void notifyPatient(Appointment appointment, String title, String type) {
        notifications.save(Notification.builder().user(appointment.getPatient().getUser()).title(title)
                .content("Lịch hẹn " + appointment.getAppointmentCode() + " ngày " + appointment.getAppointmentDate()
                        + ": " + appointment.getStatus()).type(type).referenceId(appointment.getId()).build());
    }
}
