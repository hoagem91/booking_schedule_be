package com.booking_schedule.examine.entity;

import com.booking_schedule.examine.enums.EPaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_code", length = 50, nullable = false, unique = true)
    private String paymentCode;

    // Quan hệ 1-1 với Appointment, khóa ngoại unique = true
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    // Tiền tệ sử dụng BigDecimal
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    // Phương thức thanh toán: CASH, BANK_TRANSFER, VNPAY, MOMO,...
    @Column(name = "payment_method", length = 30, nullable = false)
    private String paymentMethod;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 30, nullable = false)
    private EPaymentStatus paymentStatus = EPaymentStatus.PENDING;

    // Mã giao dịch từ cổng thanh toán bên thứ 3 (nếu có)
    @Column(name = "transaction_code")
    private String transactionCode;

    // Thời điểm người dùng thực sự thanh toán thành công
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
