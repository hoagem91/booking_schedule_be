package com.booking_schedule.examine.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
// import jakarta.validation.constraints.Max;
// import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quan hệ 1-1 với Appointment: Mỗi cuộc hẹn chỉ được đánh giá 1 lần
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    // Quan hệ N-1 với Patient: 1 bệnh nhân có thể viết nhiều đánh giá (cho các cuộc hẹn khác nhau)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // Quan hệ N-1 với Doctor: 1 bác sĩ có thể nhận nhiều đánh giá
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    // Đánh giá sao (1-5). Nếu dùng Spring Validation, có thể bỏ comment @Min và @Max bên dưới
    // @Min(value = 1, message = "Đánh giá thấp nhất là 1 sao")
    // @Max(value = 5, message = "Đánh giá cao nhất là 5 sao")
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Lob
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    // Trạng thái: VISIBLE (Hiển thị), HIDDEN (Ẩn - dành cho Admin lọc review xấu/spam)
    @Builder.Default
    @Column(name = "status", length = 30)
    private String status = "VISIBLE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}