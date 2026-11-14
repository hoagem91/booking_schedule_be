package com.booking_schedule.examine.dto.request;

import com.booking_schedule.examine.enums.EPaymentTransactionStatus;
import jakarta.validation.constraints.*;

public record PaymentTransactionSearchRequest(
        @Positive Long paymentId,
        @Pattern(regexp="VNPAY|VIETQR|PAYOS") String provider,
        EPaymentTransactionStatus status,
        @Size(max=32) String txnRef) {}
