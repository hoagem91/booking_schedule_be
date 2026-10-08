package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorScheduleCreateRequest {
    @NotNull(message = "Bác sĩ (doctorId) không được để trống")
    @Positive
    private Long doctorId;

    @Positive
    private Long roomId;

    @NotNull(message = "Ngày làm việc không được để trống")
    @FutureOrPresent(message = "Ngày làm việc không được ở trong quá khứ")
    private LocalDate workDate;

    @NotNull(message = "Giờ bắt đầu ca làm việc không được để trống")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc ca làm việc không được để trống")
    private LocalTime endTime;

    @Min(value = 1, message = "Số lượng bệnh nhân tối đa phải lớn hơn 0")
    private Integer maxPatients;

}
