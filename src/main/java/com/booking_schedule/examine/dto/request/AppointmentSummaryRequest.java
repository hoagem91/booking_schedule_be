package com.booking_schedule.examine.dto.request;

import com.booking_schedule.examine.enums.EAppointmentStatus;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentSummaryRequest {
    private String doctorKeyword;
    private String keyword;
    private Long doctorId;
    private Long patientId;
    private EAppointmentStatus status;
    private LocalDate fromDate;
    private LocalDate toDate;
}
