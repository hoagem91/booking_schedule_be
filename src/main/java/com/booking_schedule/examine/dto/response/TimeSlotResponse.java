package com.booking_schedule.examine.dto.response;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotResponse {
    private Long id;
    private Long scheduleId;

    private LocalTime startTime;
    private LocalTime endTime;

    private Integer maxPatients;
    private Integer bookedCount; // Đồng bộ tên với Entity

    private String status;
}
