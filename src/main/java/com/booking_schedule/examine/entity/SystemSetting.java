package com.booking_schedule.examine.entity;

import com.booking_schedule.examine.enums.ESettingKey;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sử dụng Enum để đảm bảo Type Safety.
    // Nếu bạn muốn lưu Key linh hoạt hoàn toàn (Admin tự gõ thêm key mới từ UI),
    // hãy đổi lại thành: private String settingKey;
    @Enumerated(EnumType.STRING)
    @Column(name = "setting_key", length = 100, nullable = false, unique = true)
    private ESettingKey settingKey;

    // Dùng @Lob và columnDefinition = "TEXT" do value có thể là chuỗi rất dài (ví dụ: chuỗi cấu hình JSON, HTML giới thiệu phòng khám)
    @Lob
    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "description", length = 500)
    private String description;

    // Bảng này trong SQL của bạn không có created_at, chỉ có updated_at
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}