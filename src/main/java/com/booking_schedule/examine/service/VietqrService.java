package com.booking_schedule.examine.service;
import com.booking_schedule.examine.configuration.VietqrProperties;
import com.booking_schedule.examine.dto.request.VietqrCallbackRequest;
import com.booking_schedule.examine.entity.*;
import com.booking_schedule.examine.enums.*;
import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor
public class VietqrService {
    private final VietqrProperties properties;
    private final VietqrAuthService auth;
    private final VietqrApiClient api;
    private final AppointmentService appointments;
    private final PaymentRepository payments;
    private final jakarta.persistence.EntityManager entityManager;
    private final PaymentTransactionRepository transactions;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public record QrResponse(String txnRef, BigDecimal amount, String content, String qrCode, String qrLink,
            String bankCode, String bankAccount, String accountName) {}
    public record StatusResponse(String txnRef, EPaymentTransactionStatus status, BigDecimal amount, LocalDateTime paidAt) {}

    @Transactional
    public QrResponse create(Long appointmentId) {
        auth.requireEnabled();
        Appointment appointment = appointments.lock(appointmentId);
        appointments.requireOwner(appointment);
        if(appointment.getStatus()!=EAppointmentStatus.PENDING && appointment.getStatus()!=EAppointmentStatus.CONFIRMED)
            throw new AppException(ErrorCode.APPOINTMENT_INVALID_TRANSITION);
        BigDecimal amount=appointment.getFinalAmount();
        if(amount==null && appointment.getPromotion()==null) amount=appointment.getServicePrice();
        if(amount==null || amount.signum()<=0 || amount.stripTrailingZeros().scale()>0)
            throw new AppException(ErrorCode.APPOINTMENT_PRICE_UNAVAILABLE);
        Payment payment=payments.findByAppointmentId(appointmentId).orElse(null);
        if(payment!=null) payment=payments.findLockedById(payment.getId()).orElseThrow();
        if(payment!=null && payment.getPaymentStatus()!=EPaymentStatus.PENDING && payment.getPaymentStatus()!=EPaymentStatus.FAILED)
            throw new AppException(ErrorCode.APPOINTMENT_INVALID_TRANSITION);
        if(payment==null) payment=Payment.builder().paymentCode("TT"+UUID.randomUUID().toString().replace("-", "").substring(0,16))
                .appointment(appointment).paymentStatus(EPaymentStatus.PENDING).build();
        payment.setAmount(amount);
        payment.setPaymentMethod("VIETQR");
        payment=payments.save(payment);
        String reference;
        do { reference=Long.toString(1_000_000_000_000L+RANDOM.nextLong(8_000_000_000_000L)); }
        while(transactions.existsByTxnRef(reference));
        // The local transaction rolls back if provider creation fails. No credentials returned to patients.
        transactions.saveAndFlush(PaymentTransaction.builder().payment(payment).txnRef(reference).amount(amount)
                .provider("VIETQR").receivingAccount(properties.getBankAccount()).status(EPaymentTransactionStatus.PENDING).build());
        var qr=api.generate(reference,amount);
        return new QrResponse(reference,amount,"KHAM"+reference,qr.path("qrCode").asText(),qr.path("qrLink").asText(),
                properties.getBankCode(),properties.getBankAccount(),properties.getUserBankName());
    }

    @Transactional(readOnly=true)
    public StatusResponse status(String reference) {
        PaymentTransaction tx=transactions.findByTxnRef(reference).orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));
        entityManager.refresh(tx, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if(!"VIETQR".equals(tx.getProvider())) throw new AppException(ErrorCode.APPOINTMENT_NOT_FOUND);
        appointments.getMine(tx.getPayment().getAppointment().getId());
        return new StatusResponse(tx.getTxnRef(),tx.getStatus(),tx.getAmount(),tx.getPaidAt());
    }

    @Transactional
    public String receive(VietqrCallbackRequest request) {
        auth.requireEnabled();
        if (request.transactiontime() > System.currentTimeMillis()+300_000L)
            throw invalid("INVALID_TRANSACTION_TIME");
        PaymentTransaction snapshot=transactions.findByTxnRef(request.orderId()).orElseThrow(() -> invalid("UNKNOWN_ORDER"));
        // Serialize against appointment cancellation and other payment attempts for this appointment.
        Appointment appointment=appointments.lock(snapshot.getPayment().getAppointment().getId());
        PaymentTransaction tx=transactions.findLockedByTxnRef(request.orderId()).orElseThrow(() -> invalid("UNKNOWN_ORDER"));
        entityManager.refresh(tx, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if(!"VIETQR".equals(tx.getProvider()) || !"C".equals(request.transType())
                || !Objects.equals(tx.getReceivingAccount(),request.bankaccount())
                || request.amount().compareTo(tx.getAmount())!=0) throw invalid("PAYMENT_MISMATCH");
        if(tx.getProviderEventId()!=null) {
            if(tx.getProviderEventId().equals(request.transactionid())
                    && Objects.equals(tx.getProviderTransactionNo(),request.referencenumber())) return tx.getTxnRef();
            throw invalid("ORDER_ALREADY_RECEIVED_DIFFERENT_EVENT");
        }
        Payment payment=payments.findLockedById(tx.getPayment().getId()).orElseThrow();
        entityManager.refresh(payment, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        tx.setProviderEventId(request.transactionid());
        tx.setProviderTransactionNo(request.referencenumber());
        tx.setPaidAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(request.transactiontime()),ZONE));
        boolean review=payment.getPaymentStatus()==EPaymentStatus.SUCCESS || payment.getPaymentStatus()==EPaymentStatus.REFUNDED
                || payment.getAmount().compareTo(tx.getAmount())!=0
                || (appointment.getStatus()!=EAppointmentStatus.PENDING && appointment.getStatus()!=EAppointmentStatus.CONFIRMED);
        tx.setStatus(review ? EPaymentTransactionStatus.REVIEW_REQUIRED : EPaymentTransactionStatus.SUCCESS);
        if(!review) {
            payment.setPaymentStatus(EPaymentStatus.SUCCESS);
            payment.setPaymentMethod("VIETQR");
            payment.setTransactionCode(request.referencenumber());
            payment.setPaidAt(tx.getPaidAt());
        }
        transactions.flush();
        return tx.getTxnRef();
    }
    private ResponseStatusException invalid(String reason) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,reason); }
}
