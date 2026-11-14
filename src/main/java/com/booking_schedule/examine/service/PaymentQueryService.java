package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.request.*;
import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.entity.*;
import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.repository.*;
import java.time.LocalDate;
import java.util.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class PaymentQueryService {
    private final PaymentRepository payments;
    private final PaymentTransactionRepository transactions;
    private final AppointmentService appointments;
    private final PayosService payos;

    public Page<PaymentResponse> mine(Pageable page) {
        Long patientId=appointments.currentPatient().getId();
        return payments.findAll((root,q,cb)->cb.equal(root.get("appointment").get("patient").get("id"),patientId),bounded(page)).map(this::payment);
    }
    public PaymentResponse mineByAppointment(Long id) {
        appointments.getMine(id);
        return payment(payments.findByAppointmentId(id).orElseThrow(this::notFound));
    }
    public PaymentResponse mineDetail(Long id) {
        var p=find(id);appointments.requireOwner(p.getAppointment());return payment(p);
    }
    public Page<PaymentTransactionResponse> mineTransactions(Long id, Pageable page) {
        var p=find(id);appointments.requireOwner(p.getAppointment());
        return adminTransactions(new PaymentTransactionSearchRequest(id,null,null,null),page);
    }
    public Page<PaymentResponse> search(PaymentSearchRequest request, Pageable page) {
        if (request.fromDate()!=null && request.toDate()!=null && request.fromDate().isAfter(request.toDate()))
            throw new AppException(ErrorCode.PAYMENT_INVALID_REQUEST);
        Specification<Payment> filter=(root,q,cb)->{
            List<Predicate> rules=new ArrayList<>();
            if (request.paymentStatus()!=null) rules.add(cb.equal(root.get("paymentStatus"),request.paymentStatus()));
            if (request.paymentMethod()!=null) rules.add(cb.equal(root.get("paymentMethod"),request.paymentMethod()));
            if (request.appointmentId()!=null) rules.add(cb.equal(root.get("appointment").get("id"),request.appointmentId()));
            if (request.fromDate()!=null) rules.add(cb.greaterThanOrEqualTo(root.get("createdAt"),request.fromDate().atStartOfDay()));
            if (request.toDate()!=null) {
                if (request.toDate().equals(LocalDate.MAX)) throw new AppException(ErrorCode.PAYMENT_INVALID_REQUEST);
                rules.add(cb.lessThan(root.get("createdAt"),request.toDate().plusDays(1).atStartOfDay()));
            }
            if (request.keyword()!=null && !request.keyword().isBlank()) {
                String value="%"+request.keyword().trim().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
                var appointment=root.join("appointment");var patient=appointment.join("patient");var user=patient.join("user");
                rules.add(cb.or(cb.like(cb.lower(root.get("paymentCode")),value,'!'),
                        cb.like(cb.lower(appointment.get("appointmentCode")),value,'!'),
                        cb.like(cb.lower(patient.get("patientCode")),value,'!'),cb.like(cb.lower(user.get("fullName")),value,'!')));
            }
            return cb.and(rules.toArray(Predicate[]::new));
        };
        return payments.findAll(filter,bounded(page)).map(this::payment);
    }
    public PaymentResponse adminDetail(Long id) { return payment(find(id)); }
    public Page<PaymentTransactionResponse> adminTransactions(PaymentTransactionSearchRequest request, Pageable page) {
        Specification<PaymentTransaction> filter=(root,q,cb)->{
            List<Predicate> rules=new ArrayList<>();
            if (request.paymentId()!=null) rules.add(cb.equal(root.get("payment").get("id"),request.paymentId()));
            if (request.provider()!=null) {
                var equals=cb.equal(root.get("provider"),request.provider());
                rules.add("VNPAY".equals(request.provider()) ? cb.or(equals,cb.isNull(root.get("provider"))) : equals);
            }
            if (request.status()!=null) rules.add(cb.equal(root.get("status"),request.status()));
            if (request.txnRef()!=null && !request.txnRef().isBlank()) rules.add(cb.equal(root.get("txnRef"),request.txnRef().trim()));
            return cb.and(rules.toArray(Predicate[]::new));
        };
        return transactions.findAll(filter,bounded(page)).map(this::transaction);
    }
    public PaymentTransactionResponse adminTransaction(String reference) {
        return transaction(transactions.findByTxnRef(reference).orElseThrow(this::notFound));
    }
    // Network and settlement happen outside this read-only query service transaction.
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public PayosPaymentStore.Status adminRefresh(String reference) {
        var tx=transactions.findByTxnRef(reference).orElseThrow(this::notFound);
        if (!"PAYOS".equals(tx.getProvider())) throw new AppException(ErrorCode.PAYMENT_PROVIDER_UNSUPPORTED);
        return payos.adminRefresh(reference);
    }
    private Payment find(Long id) { return payments.findById(id).orElseThrow(this::notFound); }
    private AppException notFound() { return new AppException(ErrorCode.PAYMENT_NOT_FOUND); }
    private Pageable bounded(Pageable page) {
        // Stable order prevents duplicate/missing rows during FE pagination; arbitrary nested sort is not exposed.
        return PageRequest.of(Math.max(0,page.getPageNumber()),Math.min(100,Math.max(1,page.getPageSize())),Sort.by(Sort.Direction.DESC,"id"));
    }
    private PaymentResponse payment(Payment p) {
        var a=p.getAppointment();var patient=a.getPatient();var user=patient.getUser();
        return PaymentResponse.builder().id(p.getId()).paymentCode(p.getPaymentCode()).appointmentId(a.getId())
                .appointmentCode(a.getAppointmentCode()).patientCode(patient.getPatientCode()).patientName(user.getFullName())
                .patientPhone(user.getPhone()).amount(p.getAmount()).paymentMethod(p.getPaymentMethod()).paymentStatus(p.getPaymentStatus())
                .transactionCode(p.getTransactionCode()).paidAt(p.getPaidAt()).createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }
    private PaymentTransactionResponse transaction(PaymentTransaction tx) {
        return new PaymentTransactionResponse(tx.getId(),tx.getPayment().getId(),tx.getPayment().getAppointment().getId(),tx.getTxnRef(),
                tx.getProvider()==null ? "VNPAY" : tx.getProvider(),tx.getAmount(),tx.getStatus(),tx.getTransactionStatus(),
                tx.getProviderTransactionNo(),tx.getExpiresAt(),tx.getPaidAt(),tx.getCreatedAt());
    }
}
