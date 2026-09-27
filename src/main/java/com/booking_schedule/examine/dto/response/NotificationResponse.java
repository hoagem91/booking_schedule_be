package com.booking_schedule.examine.dto.response;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;

    // ID của User sở hữu thông báo (cần thiết khi Admin xem log thông báo)
    private Long userId;

    private String title;
    private String content;
    private String type;
    private Boolean isRead;

    // Dùng để Frontend điều hướng (Ví dụ: click vào thông báo sẽ nhảy sang trang chi tiết Appointment)
    private Long referenceId;

    private LocalDateTime createdAt;
}
