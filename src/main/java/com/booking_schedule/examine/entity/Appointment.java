package com.booking_schedule.examine.entity;

import com.booking_schedule.examine.enums.EAppointmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_code", length = 50, nullable = false, unique = true)
    private String appointmentCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    // Giữ giá niêm yết tại thời điểm đặt lịch, độc lập với giá hiện tại của Service.
    // Cho phép null cho dữ liệu cũ chưa có thông tin giá lịch sử.
    @Column(name = "service_price", precision = 15, scale = 2, updatable = false)
    private BigDecimal servicePrice;

    // Snapshot sau giảm giá; không tính lại từ Promotion/Service có thể đã thay đổi.
    @Column(name = "final_amount", precision = 15, scale = 2, updatable = false)
    private BigDecimal finalAmount;

    @PrePersist
    void captureServicePrice() {
        if (service != null) {
            servicePrice = service.getPrice();
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Lob
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private EAppointmentStatus status = EAppointmentStatus.PENDING;

    @Lob
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
