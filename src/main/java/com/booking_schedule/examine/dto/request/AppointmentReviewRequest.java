package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentReviewRequest {
    @NotNull @Min(1) @Max(5)
    private Integer rating;
    @Size(max = 2000)
    private String comment;
}
