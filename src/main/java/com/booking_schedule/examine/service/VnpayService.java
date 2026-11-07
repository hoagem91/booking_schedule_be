package com.booking_schedule.examine.service;

import com.booking_schedule.examine.configuration.VnpayProperties;
import com.booking_schedule.examine.dto.response.VnpayPaymentLinkResponse;
import com.booking_schedule.examine.entity.*;
import com.booking_schedule.examine.enums.*;
import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VnpayService {
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();
    private final VnpayProperties properties;
    private final PaymentRepository payments;
    private final PaymentTransactionRepository transactions;
    private final AppointmentService appointments;

    @Transactional
    public VnpayPaymentLinkResponse createPaymentLink(Long appointmentId, String clientIp) {
        Appointment appointment = appointments.lock(appointmentId);
        appointments.requireOwner(appointment);
        if (appointment.getStatus() != EAppointmentStatus.PENDING && appointment.getStatus() != EAppointmentStatus.CONFIRMED)
            throw new AppException(ErrorCode.APPOINTMENT_INVALID_TRANSITION);
        BigDecimal amount = appointment.getFinalAmount();
        if (amount == null && appointment.getPromotion() == null) amount = appointment.getServicePrice();
        if (amount == null || amount.signum() <= 0)
            throw new AppException(ErrorCode.APPOINTMENT_PRICE_UNAVAILABLE);
        if (properties.getTmnCode() == null || properties.getTmnCode().isBlank()
                || properties.getHashSecret() == null || properties.getHashSecret().isBlank())
            throw new IllegalStateException("VNPAY credentials are not configured");

        BigDecimal finalAmount = amount;
        Payment payment = payments.findByAppointmentId(appointmentId).orElseGet(() -> payments.save(Payment.builder()
                .paymentCode("TT" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT))
                .appointment(appointment).amount(finalAmount).paymentMethod("VNPAY")
                .paymentStatus(EPaymentStatus.PENDING).build()));
        if (payment.getPaymentStatus() == EPaymentStatus.SUCCESS)
            throw new AppException(ErrorCode.APPOINTMENT_INVALID_TRANSITION);
        payment.setAmount(amount);
        payment.setPaymentMethod("VNPAY");
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDateTime expires = now.plusMinutes(Math.max(1, properties.getExpireMinutes()));
        String txnRef = newTxnRef();
        PaymentTransaction tx = transactions.save(PaymentTransaction.builder().payment(payment).txnRef(txnRef)
                .amount(amount).status(EPaymentTransactionStatus.PENDING).expiresAt(expires).build());
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", properties.getTmnCode());
        params.put("vnp_Amount", amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).toPlainString());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", tx.getTxnRef());
        params.put("vnp_OrderInfo", "Thanh toan lich kham " + appointment.getAppointmentCode());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", properties.getReturnUrl());
        params.put("vnp_IpAddr", normalizeIp(clientIp));
        params.put("vnp_CreateDate", VNPAY_DATE.format(now));
        params.put("vnp_ExpireDate", VNPAY_DATE.format(expires));
        String signData = encodeParams(params);
        params.put("vnp_SecureHash", hmacSha512(properties.getHashSecret(), signData));
        String url = properties.getPayUrl() + "?" + params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue())).collect(Collectors.joining("&"));
        return VnpayPaymentLinkResponse.builder().txnRef(txnRef).paymentUrl(url).amount(amount).expiresAt(expires).build();
    }

    @Transactional
    public String processIpn(Map<String, String> params) {
        if (!verify(params)) return "97";
        if (!properties.getTmnCode().equals(params.get("vnp_TmnCode"))) return "97";
        PaymentTransaction tx = transactions.findLockedByTxnRef(params.get("vnp_TxnRef")).orElse(null);
        if (tx == null) return "01";
        BigDecimal vnpAmount = new BigDecimal(params.getOrDefault("vnp_Amount", "-1")).movePointLeft(2);
        if (vnpAmount.compareTo(tx.getAmount()) != 0) return "04";
        if (tx.getStatus() != EPaymentTransactionStatus.PENDING) return "02";
        boolean succeeded = "00".equals(params.get("vnp_ResponseCode")) && "00".equals(params.get("vnp_TransactionStatus"));
        if (!succeeded && tx.getExpiresAt() != null && LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).isAfter(tx.getExpiresAt())) {
            tx.setStatus(EPaymentTransactionStatus.EXPIRED);
            tx.setResponseCode(params.get("vnp_ResponseCode"));
            tx.setTransactionStatus(params.get("vnp_TransactionStatus"));
            return "00";
        }
        String response = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        tx.setResponseCode(response);
        tx.setTransactionStatus(transactionStatus);
        tx.setProviderTransactionNo(params.get("vnp_TransactionNo"));
        if ("00".equals(response) && "00".equals(transactionStatus)) {
            tx.setStatus(EPaymentTransactionStatus.SUCCESS);
            tx.setPaidAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            Payment payment = tx.getPayment();
            payment.setPaymentStatus(EPaymentStatus.SUCCESS);
            payment.setTransactionCode(params.get("vnp_TransactionNo"));
            payment.setPaidAt(tx.getPaidAt());
        } else {
            tx.setStatus(EPaymentTransactionStatus.FAILED);
        }
        return "00";
    }

    public boolean isValidReturn(Map<String, String> params) {
        return verify(params) && properties.getTmnCode().equals(params.get("vnp_TmnCode"));
    }

    private boolean verify(Map<String, String> params) {
        String received = params.get("vnp_SecureHash");
        if (received == null || properties.getHashSecret() == null) return false;
        Map<String, String> signed = new TreeMap<>(params);
        signed.remove("vnp_SecureHash");
        signed.remove("vnp_SecureHashType");
        try {
            byte[] expected = hmacSha512(properties.getHashSecret(), encodeParams(signed)).getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(expected, received.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
        } catch (RuntimeException ex) { return false; }
    }

    private String encodeParams(Map<String, String> params) {
        return params.entrySet().stream().filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue())).collect(Collectors.joining("&"));
    }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private String newTxnRef() {
        String value;
        do { value = Long.toString(1_000_000_000_000L + RANDOM.nextLong(8_000_000_000_000L)); }
        while (transactions.existsByTxnRef(value));
        return value;
    }
    private String normalizeIp(String ip) { return ip == null || ip.isBlank() ? "127.0.0.1" : ip.split(",")[0].trim(); }
    private String hmacSha512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException("Unable to sign VNPAY request", ex); }
    }
}
