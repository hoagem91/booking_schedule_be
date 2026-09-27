package com.booking_schedule.examine.dto.response;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;

    // Phẳng hóa thông tin Cuộc hẹn
    private Long appointmentId;
    private String appointmentCode;

    // Phẳng hóa thông tin Bệnh nhân viết đánh giá
    private Long patientId;
    private String patientName;

    // Phẳng hóa thông tin Bác sĩ nhận đánh giá
    private Long doctorId;
    private String doctorName;

    private Integer rating;
    private String comment;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
