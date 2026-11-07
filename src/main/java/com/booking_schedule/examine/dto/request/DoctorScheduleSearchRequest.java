package com.booking_schedule.examine.dto.request;

import com.booking_schedule.examine.enums.EStatusDoctorSchedule;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorScheduleSearchRequest {
    private Long doctorId;
    private Long specialtyId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private EStatusDoctorSchedule status;
    private List<EStatusDoctorSchedule> statuses;
}
