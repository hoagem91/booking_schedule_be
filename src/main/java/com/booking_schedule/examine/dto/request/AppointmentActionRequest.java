package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentActionRequest {
    @Size(max = 2000)
    private String note;
}
