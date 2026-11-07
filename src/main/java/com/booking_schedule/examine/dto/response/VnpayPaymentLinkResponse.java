package com.booking_schedule.examine.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder
public class VnpayPaymentLinkResponse {
    private String txnRef;
    private String paymentUrl;
    private BigDecimal amount;
    private LocalDateTime expiresAt;
}
