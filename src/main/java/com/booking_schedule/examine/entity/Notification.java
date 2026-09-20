package com.booking_schedule.examine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quan hệ N-1: Nhiều thông báo gửi đến 1 User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false)
    private String title;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // Loại thông báo (Ví dụ: APPOINTMENT_REMINDER, SYSTEM_ALERT, PROMOTION)
    // Bạn có thể tạo thêm Enum cho trường này nếu muốn kiểm soát chặt chẽ
    @Column(name = "type", length = 50)
    private String type;

    @Builder.Default
    @Column(name = "is_read")
    private Boolean isRead = false;

    // Soft Reference: Lưu ID của đối tượng liên quan (ID của Appointment, Payment,...)
    // Không dùng @ManyToOne vì id này có thể trỏ tới nhiều bảng khác nhau dựa theo 'type'
    @Column(name = "reference_id")
    private Long referenceId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}
