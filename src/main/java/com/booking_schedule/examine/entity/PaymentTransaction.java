package com.booking_schedule.examine.entity;

import com.booking_schedule.examine.enums.EPaymentTransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions", indexes = @Index(name = "idx_payment_txn_payment", columnList = "payment_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "txn_ref", length = 32, nullable = false, unique = true)
    private String txnRef;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private EPaymentTransactionStatus status = EPaymentTransactionStatus.PENDING;

    @Column(name = "provider_transaction_no", length = 100)
    private String providerTransactionNo;

    @Column(name = "response_code", length = 10)
    private String responseCode;

    @Column(name = "transaction_status", length = 10)
    private String transactionStatus;

    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
