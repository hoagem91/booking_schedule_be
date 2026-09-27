package com.booking_schedule.examine.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCreateRequest {
    @NotNull(message = "ID người nhận (userId) không được để trống")
    private Long userId;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không vượt quá 255 ký tự")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    @Size(max = 50, message = "Loại thông báo không vượt quá 50 ký tự")
    private String type; // APPOINTMENT_REMINDER, PROMOTION, SYSTEM_ALERT...

    // Có thể null nếu đây là thông báo hệ thống chung chung
    private Long referenceId;
}
