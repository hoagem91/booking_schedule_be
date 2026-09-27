package com.booking_schedule.examine.dto.request;

import com.booking_schedule.examine.enums.EAppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentStatusUpdateRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private EAppointmentStatus status;

    // Có thể dùng để ghi lại lý do hủy lịch
    private String note;
}
