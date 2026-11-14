package com.booking_schedule.examine.dto.request;

import com.booking_schedule.examine.enums.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record PaymentSearchRequest(
        @Size(max=100) String keyword,
        EPaymentStatus paymentStatus,
        @Pattern(regexp="CASH|VNPAY|VIETQR|PAYOS") String paymentMethod,
        @Positive Long appointmentId,
        LocalDate fromDate, LocalDate toDate) {}
