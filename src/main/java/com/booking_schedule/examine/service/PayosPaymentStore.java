package com.booking_schedule.examine.service;

import com.booking_schedule.examine.configuration.PayosProperties;
import com.booking_schedule.examine.entity.*;
import com.booking_schedule.examine.enums.*;
import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.payos.model.v2.paymentRequests.*;
import vn.payos.model.webhooks.WebhookData;

@Service @RequiredArgsConstructor
public class PayosPaymentStore {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final SecureRandom RANDOM = new SecureRandom();
    private final PayosProperties properties;
    private final AppointmentService appointments;
    private final PaymentRepository payments;
    private final PaymentTransactionRepository transactions;
    private final EntityManager entityManager;
    public record Checkout(String txnRef, BigDecimal amount, String paymentLinkId, String checkoutUrl,
            String qrCode, LocalDateTime expiresAt, @com.fasterxml.jackson.annotation.JsonIgnore boolean newlyCreated) {}
    public record Status(String txnRef, EPaymentTransactionStatus status, String providerStatus,
            BigDecimal amount, LocalDateTime paidAt) {}

    // Reserve before network I/O: a timeout must not erase the order that payOS may have created.
    @Transactional
    public Checkout reserve(Long appointmentId) {
        Appointment appointment = appointments.lock(appointmentId);
        appointments.requireOwner(appointment);
        if (!payable(appointment))
            throw new AppException(ErrorCode.APPOINTMENT_INVALID_TRANSITION);
        BigDecimal amount=appointment.getFinalAmount();
        if (amount==null && appointment.getPromotion()==null) amount=appointment.getServicePrice();
        if (amount==null || amount.signum()<=0 || amount.stripTrailingZeros().scale()>0)
            throw new AppException(ErrorCode.APPOINTMENT_PRICE_UNAVAILABLE);
        Payment payment=payments.findByAppointmentId(appointmentId).orElse(null);
        if (payment!=null) {
            payment=payments.findLockedById(payment.getId()).orElseThrow();
            entityManager.refresh(payment,LockModeType.PESSIMISTIC_WRITE);
            if (payment.getPaymentStatus()!=EPaymentStatus.PENDING && payment.getPaymentStatus()!=EPaymentStatus.FAILED)
                throw new AppException(ErrorCode.APPOINTMENT_INVALID_TRANSITION);
        }
        var previous=transactions.findFirstByPaymentAppointmentIdAndProviderAndStatusOrderByIdDesc(
                appointmentId,"PAYOS",EPaymentTransactionStatus.PENDING);
        if (previous.isPresent()) {
            var tx=previous.get();
            if (tx.getExpiresAt()!=null && tx.getExpiresAt().isAfter(LocalDateTime.now(ZONE))) {
                if (tx.getAmount().compareTo(amount)!=0) throw invalid("EXISTING_PAYOS_ORDER_AMOUNT_CHANGED");
                return checkout(tx,false);
            }
            tx.setStatus(EPaymentTransactionStatus.EXPIRED);
        }
        if (payment==null) payment=Payment.builder().paymentCode("TT"+UUID.randomUUID().toString().replace("-", "").substring(0,16))
                .appointment(appointment).paymentStatus(EPaymentStatus.PENDING).build();
        payment.setAmount(amount);
        payment.setPaymentMethod("PAYOS");
        payment=payments.save(payment);
        String reference;
        do { reference=Long.toString(1_000_000_000_000L+RANDOM.nextLong(8_000_000_000_000L)); }
        while (transactions.existsByTxnRef(reference));
        var tx=transactions.saveAndFlush(PaymentTransaction.builder().payment(payment).txnRef(reference)
                .provider("PAYOS").amount(amount).status(EPaymentTransactionStatus.PENDING)
                .expiresAt(LocalDateTime.now(ZONE).plusMinutes(properties.getExpireMinutes())).build());
        return checkout(tx,true);
    }
    @Transactional
    public Checkout attach(String reference, CreatePaymentLinkResponse response) {
        var snapshot=find(reference);
        appointments.lock(snapshot.getPayment().getAppointment().getId());
        var tx=locked(reference);
        if (response==null || !reference.equals(String.valueOf(response.getOrderCode()))
                || response.getAmount()==null || tx.getAmount().compareTo(BigDecimal.valueOf(response.getAmount()))!=0
                || !"VND".equals(response.getCurrency()) || response.getPaymentLinkId()==null
                || response.getPaymentLinkId().isBlank() || response.getPaymentLinkId().length()>100
                || response.getAccountNumber()==null || response.getAccountNumber().isBlank() || response.getAccountNumber().length()>100
                || response.getCheckoutUrl()==null || !response.getCheckoutUrl().startsWith("https://")
                || response.getCheckoutUrl().length()>1024 || response.getQrCode()==null || response.getQrCode().isBlank())
            throw invalid("INVALID_PAYOS_CREATE_RESPONSE");
        if (tx.getProviderPaymentLinkId()!=null && !tx.getProviderPaymentLinkId().equals(response.getPaymentLinkId()))
            throw invalid("PAYOS_LINK_MISMATCH");
        tx.setProviderPaymentLinkId(response.getPaymentLinkId());
        tx.setReceivingAccount(response.getAccountNumber());
        tx.setCheckoutUrl(response.getCheckoutUrl());
        tx.setQrCode(response.getQrCode());
        return checkout(tx,false);
    }
    @Transactional(readOnly=true)
    public Status owned(String reference) {
        var tx=find(reference);
        appointments.getMine(tx.getPayment().getAppointment().getId());
        return status(tx);
    }
    @Transactional(readOnly=true)
    public boolean accepts(WebhookData event) {
        var found=transactions.findByTxnRef(String.valueOf(event.getOrderCode()));
        // payOS sends a signed example when registering a webhook. Acknowledge unknown orders, never settle them.
        if (found.isEmpty()) return false;
        var tx=found.get();
        if (!"PAYOS".equals(tx.getProvider())) throw invalid("PAYOS_PROVIDER_MISMATCH");
        if (tx.getProviderPaymentLinkId()!=null && !tx.getProviderPaymentLinkId().equals(event.getPaymentLinkId()))
            throw invalid("PAYOS_LINK_MISMATCH");
        // QR account may be a virtual account while webhook accountNumber is the linked bank account.
        // The verified event only triggers a GET of this exact order/link using our channel credentials.
        // Settlement is based on that authoritative response, never on this event alone.
        return true;
    }
    @Transactional
    public Status apply(String reference, PaymentLink remote) {
        var snapshot=find(reference);
        Appointment appointment=appointments.lock(snapshot.getPayment().getAppointment().getId());
        entityManager.refresh(appointment,LockModeType.PESSIMISTIC_WRITE);
        var tx=locked(reference);
        if (remote==null || !reference.equals(String.valueOf(remote.getOrderCode())) || remote.getId()==null
                || remote.getId().isBlank() || remote.getId().length()>100 || remote.getAmount()==null
                || tx.getAmount().compareTo(BigDecimal.valueOf(remote.getAmount()))!=0
                || remote.getStatus()==null || (tx.getProviderPaymentLinkId()!=null && !tx.getProviderPaymentLinkId().equals(remote.getId())))
            throw invalid("PAYOS_ORDER_MISMATCH");
        tx.setProviderPaymentLinkId(remote.getId());
        tx.setTransactionStatus(remote.getStatus().name());
        if (remote.getStatus()!=PaymentLinkStatus.PAID) {
            if (tx.getStatus()!=EPaymentTransactionStatus.SUCCESS && tx.getStatus()!=EPaymentTransactionStatus.REVIEW_REQUIRED) {
                if (remote.getStatus()==PaymentLinkStatus.CANCELLED || remote.getStatus()==PaymentLinkStatus.FAILED)
                    tx.setStatus(EPaymentTransactionStatus.FAILED);
                if (remote.getStatus()==PaymentLinkStatus.EXPIRED) tx.setStatus(EPaymentTransactionStatus.EXPIRED);
            }
            return status(tx);
        }
        if (remote.getAmountPaid()==null || remote.getAmountPaid()<remote.getAmount()
                || remote.getAmountRemaining()==null || remote.getAmountRemaining()>0
                || (remote.getAmountRemaining()<0 && remote.getAmountPaid().equals(remote.getAmount()))
                || remote.getTransactions()==null || remote.getTransactions().isEmpty()) throw invalid("PAYOS_PAID_DATA_INCOMPLETE");
        BigDecimal total=BigDecimal.ZERO;
        LocalDateTime paidAt=null;
        for (var item:remote.getTransactions()) {
            if (item.getAmount()==null || item.getAmount()<=0 || item.getReference()==null
                    || item.getReference().isBlank() || item.getReference().length()>100 || item.getAccountNumber()==null
                    || item.getAccountNumber().isBlank() || item.getAccountNumber().length()>100)
                throw invalid("PAYOS_TRANSACTION_MISMATCH");
            total=total.add(BigDecimal.valueOf(item.getAmount()));
            LocalDateTime time=parsePaymentTime(item.getTransactionDateTime());
            if (paidAt==null || time.isAfter(paidAt)) paidAt=time;
        }
        if (total.compareTo(BigDecimal.valueOf(remote.getAmountPaid()))!=0) throw invalid("PAYOS_TOTAL_MISMATCH");
        if (tx.getReceivingAccount()==null) tx.setReceivingAccount(remote.getTransactions().getFirst().getAccountNumber());
        boolean excess=total.compareTo(tx.getAmount())!=0;
        if (tx.getStatus()==EPaymentTransactionStatus.SUCCESS && !excess) return status(tx);
        if (tx.getStatus()==EPaymentTransactionStatus.REVIEW_REQUIRED) return status(tx);
        Payment payment=payments.findLockedById(tx.getPayment().getId()).orElseThrow();
        entityManager.refresh(payment,LockModeType.PESSIMISTIC_WRITE);
        boolean review=excess || payment.getPaymentStatus()==EPaymentStatus.SUCCESS || payment.getPaymentStatus()==EPaymentStatus.REFUNDED
                || payment.getAmount().compareTo(tx.getAmount())!=0
                || !payable(appointment);
        tx.setProviderEventId(remote.getId());
        tx.setProviderTransactionNo(remote.getTransactions().getLast().getReference());
        tx.setPaidAt(paidAt);
        tx.setStatus(review ? EPaymentTransactionStatus.REVIEW_REQUIRED : EPaymentTransactionStatus.SUCCESS);
        if (!review) {
            payment.setPaymentStatus(EPaymentStatus.SUCCESS);
            payment.setPaymentMethod("PAYOS");
            payment.setPaidAt(paidAt);
            payment.setTransactionCode(tx.getProviderTransactionNo());
        }
        transactions.flush();
        return status(tx);
    }
    static LocalDateTime parsePaymentTime(String value) {
        if (value == null || value.isBlank())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"PAYOS_TRANSACTION_TIME_INVALID");
        String normalized=value.trim().replace(' ', 'T');
        try {
            // Offset-bearing values represent an instant; store consistently in the clinic timezone.
            return OffsetDateTime.parse(normalized).atZoneSameInstant(ZONE).toLocalDateTime();
        } catch (java.time.format.DateTimeParseException withoutOffset) {
            try {
                // Legacy webhook/API values without offset are local clinic time; optional fractions supported.
                return LocalDateTime.parse(normalized,DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (java.time.format.DateTimeParseException invalid) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,"PAYOS_TRANSACTION_TIME_INVALID");
            }
        }
    }
    private boolean payable(Appointment appointment) {
        return appointment.getStatus()==EAppointmentStatus.PENDING || appointment.getStatus()==EAppointmentStatus.CONFIRMED
                || appointment.getStatus()==EAppointmentStatus.COMPLETED;
    }
    private PaymentTransaction find(String reference) {
        var tx=transactions.findByTxnRef(reference).orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));
        if (!"PAYOS".equals(tx.getProvider())) throw new AppException(ErrorCode.APPOINTMENT_NOT_FOUND);
        return tx;
    }
    private PaymentTransaction locked(String reference) {
        var tx=transactions.findLockedByTxnRef(reference).orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));
        // A previous lookup may already be in the persistence context before waiting for a lock.
        entityManager.refresh(tx,LockModeType.PESSIMISTIC_WRITE);
        if (!"PAYOS".equals(tx.getProvider())) throw new AppException(ErrorCode.APPOINTMENT_NOT_FOUND);
        return tx;
    }
    private Checkout checkout(PaymentTransaction tx, boolean created) {
        return new Checkout(tx.getTxnRef(),tx.getAmount(),tx.getProviderPaymentLinkId(),tx.getCheckoutUrl(),tx.getQrCode(),tx.getExpiresAt(),created);
    }
    private Status status(PaymentTransaction tx) { return new Status(tx.getTxnRef(),tx.getStatus(),tx.getTransactionStatus(),tx.getAmount(),tx.getPaidAt()); }
    private ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
}
