package com.booking_schedule.examine.dto.request;

import com.booking_schedule.examine.enums.EStatusDoctorSchedule;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DoctorScheduleStatusRequest {
    @NotNull(message = "Trạng thái ca không được để trống")
    private EStatusDoctorSchedule status;
}
