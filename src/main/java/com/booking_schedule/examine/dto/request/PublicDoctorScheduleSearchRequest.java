package com.booking_schedule.examine.dto.request;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicDoctorScheduleSearchRequest {
    private Long doctorId;
    private Long specialtyId;
    private LocalDate fromDate;
    private LocalDate toDate;
}
