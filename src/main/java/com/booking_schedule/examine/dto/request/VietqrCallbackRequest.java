package com.booking_schedule.examine.dto.request;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record VietqrCallbackRequest(
    @NotBlank @Size(max=100) String bankaccount,
    @NotNull @DecimalMin("1") @Digits(integer=13,fraction=0) BigDecimal amount,
    @NotBlank @Pattern(regexp="C|D") String transType,
    @NotBlank @Size(max=500) String content,
    @NotBlank @Size(max=100) String transactionid,
    @NotNull @Positive Long transactiontime,
    @NotBlank @Size(max=100) String referencenumber,
    @NotBlank @Size(max=32) String orderId) {}
