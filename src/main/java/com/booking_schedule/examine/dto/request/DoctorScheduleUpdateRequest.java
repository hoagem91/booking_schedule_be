package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorScheduleUpdateRequest {
    private Long roomId;

    @FutureOrPresent(message = "Ngày làm việc không được ở trong quá khứ")
    private LocalDate workDate;

    private LocalTime startTime;
    private LocalTime endTime;

    @Min(value = 1, message = "Số lượng bệnh nhân tối đa phải lớn hơn 0")
    private Integer maxPatients;

    private String status;
}
