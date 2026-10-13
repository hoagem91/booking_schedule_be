package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.request.AppointmentReviewRequest;
import com.booking_schedule.examine.dto.response.ReviewResponse;
import com.booking_schedule.examine.entity.*;
import com.booking_schedule.examine.enums.EAppointmentStatus;
import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentReviewService {
    private final AppointmentService appointments;
    private final ReviewRepository reviews;

    @Transactional
    public ReviewResponse create(Long id, AppointmentReviewRequest request) {
        Appointment appointment = appointments.lock(id);
        appointments.requireOwner(appointment);
        if (appointment.getStatus() != EAppointmentStatus.COMPLETED || reviews.findLockedByAppointmentId(id).isPresent())
            throw new AppException(ErrorCode.REVIEW_NOT_ALLOWED);
        Review review = reviews.saveAndFlush(Review.builder().appointment(appointment).patient(appointment.getPatient())
                .doctor(appointment.getDoctor()).rating(request.getRating()).comment(request.getComment()).build());
        return ReviewResponse.builder().id(review.getId()).appointmentId(id).appointmentCode(appointment.getAppointmentCode())
                .patientId(appointment.getPatient().getId()).patientName(appointment.getPatient().getUser().getFullName())
                .doctorId(appointment.getDoctor().getId()).doctorName(appointment.getDoctor().getUser().getFullName())
                .rating(review.getRating()).comment(review.getComment()).status(review.getStatus())
                .createdAt(review.getCreatedAt()).updatedAt(review.getUpdatedAt()).build();
    }
}
