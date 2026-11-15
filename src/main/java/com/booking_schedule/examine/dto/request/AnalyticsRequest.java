package com.booking_schedule.examine.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AnalyticsRequest {
    private LocalDate fromDate;

    private LocalDate toDate;
}
