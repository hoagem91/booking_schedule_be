package com.booking_schedule.examine.entity;

import com.booking_schedule.examine.enums.EPaymentTransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions", indexes = @Index(name = "idx_payment_txn_payment", columnList = "payment_id"), uniqueConstraints = @UniqueConstraint(name="uk_payment_provider_event", columnNames={"provider", "provider_event_id"}))
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

    @Column(length = 20)
    private String provider;
    @Column(length = 100)
    private String receivingAccount;
    @Column(name="provider_event_id", length = 100)
    private String providerEventId;
    @Column(name="provider_payment_link_id", length=100)
    private String providerPaymentLinkId;
    @Column(name="checkout_url", length=1024)
    private String checkoutUrl;
    @Column(name="qr_code", columnDefinition="TEXT")
    private String qrCode;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
