package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorScheduleUpdateRequest {
    // Optional assertion of the schedule's doctor; this field does not reassign the doctor.
    @Positive(message = "doctorId phải lớn hơn 0")
    private Long doctorId;

    @Positive
    private Long roomId;

    @FutureOrPresent(message = "Ngày làm việc không được ở trong quá khứ")
    private LocalDate workDate;

    private LocalTime startTime;
    private LocalTime endTime;

    @Min(value = 1, message = "Số lượng bệnh nhân tối đa phải lớn hơn 0")
    private Integer maxPatients;

}
