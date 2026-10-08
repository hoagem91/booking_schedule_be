package com.booking_schedule.examine.dto.request;

import com.booking_schedule.examine.enums.ETimeSlotStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TimeSlotStatusRequest {
    @NotNull(message = "Trạng thái khung giờ không được để trống")
    private ETimeSlotStatus status;
}
