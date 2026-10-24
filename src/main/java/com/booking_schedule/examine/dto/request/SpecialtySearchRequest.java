package com.booking_schedule.examine.dto.request;

import com.booking_schedule.examine.enums.EStatus;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SpecialtySearchRequest {
    private String keyword;
    private EStatus status;
}
