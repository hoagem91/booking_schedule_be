package com.booking_schedule.examine.dto.response;

import com.booking_schedule.examine.enums.EPaymentTransactionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder
public class VnpayTransactionStatusResponse {
    private String txnRef;
    private EPaymentTransactionStatus status;
    private BigDecimal amount;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
}
