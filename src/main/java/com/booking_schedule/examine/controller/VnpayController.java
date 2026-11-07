package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.configuration.VnpayProperties;
import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.entity.PaymentTransaction;
import com.booking_schedule.examine.repository.PaymentTransactionRepository;
import com.booking_schedule.examine.service.AppointmentService;
import com.booking_schedule.examine.service.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class VnpayController {
    private final VnpayService vnpay;
    private final PaymentTransactionRepository transactions;
    private final VnpayProperties properties;
    private final AppointmentService appointments;

    @PostMapping("/api/patient/appointments/{id}/payments/vnpay")
    public ApiResponse<VnpayPaymentLinkResponse> create(@PathVariable Long id, HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return ApiResponse.<VnpayPaymentLinkResponse>builder()
                .result(vnpay.createPaymentLink(id, ip)).build();
    }

    @GetMapping("/api/patient/payments/vnpay/{txnRef}")
    public ApiResponse<VnpayTransactionStatusResponse> status(@PathVariable String txnRef) {
        PaymentTransaction tx = transactions.findByTxnRef(txnRef).orElseThrow(
                () -> new com.booking_schedule.examine.exception.AppException(
                        com.booking_schedule.examine.exception.ErrorCode.APPOINTMENT_NOT_FOUND));
        appointments.getMine(tx.getPayment().getAppointment().getId());
        return ApiResponse.<VnpayTransactionStatusResponse>builder().result(VnpayTransactionStatusResponse.builder()
                .txnRef(tx.getTxnRef()).status(tx.getStatus()).amount(tx.getAmount())
                .expiresAt(tx.getExpiresAt()).paidAt(tx.getPaidAt()).build()).build();
    }

    @GetMapping("/api/payments/vnpay/ipn")
    public ResponseEntity<VnpayIpnResponse> ipn(@RequestParam Map<String, String> params) {
        String code = vnpay.processIpn(params);
        String message = switch (code) {
            case "00" -> "Confirm Success";
            case "01" -> "Order not Found";
            case "02" -> "Order already confirmed";
            case "04" -> "Invalid Amount";
            case "97" -> "Invalid Checksum";
            default -> "Unknown error";
        };
        HttpStatus status = "99".equals(code) ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.OK;
        return ResponseEntity.status(status).body(new VnpayIpnResponse(code, message));
    }

    @GetMapping("/api/payments/vnpay/return")
    public RedirectView paymentReturn(@RequestParam Map<String, String> params) {
        String ref = params.get("vnp_TxnRef");
        String result = "INVALID";
        if (vnpay.isValidReturn(params) && ref != null) {
            Optional<PaymentTransaction> tx = transactions.findByTxnRef(ref);
            result = tx.map(t -> t.getStatus().name()).orElse("PENDING");
            if ("PENDING".equals(result) && "00".equals(params.get("vnp_ResponseCode"))
                    && "00".equals(params.get("vnp_TransactionStatus"))) result = "PROCESSING";
        }
        String target = org.springframework.web.util.UriComponentsBuilder.fromUriString(properties.getFrontendResultUrl())
                .queryParam("txnRef", ref == null ? "" : ref)
                .queryParam("status", result).build().encode().toUriString();
        return new RedirectView(target);
    }
}
