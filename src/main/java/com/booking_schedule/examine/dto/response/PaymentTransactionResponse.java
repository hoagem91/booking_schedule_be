package com.booking_schedule.examine.dto.response;

import com.booking_schedule.examine.enums.EPaymentTransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransactionResponse(Long id, Long paymentId, Long appointmentId, String txnRef,
        String provider, BigDecimal amount, EPaymentTransactionStatus status, String providerStatus,
        String providerTransactionNo, LocalDateTime expiresAt, LocalDateTime paidAt, LocalDateTime createdAt) {}
