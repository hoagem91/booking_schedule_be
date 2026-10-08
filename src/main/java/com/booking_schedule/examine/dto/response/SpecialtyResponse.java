package com.booking_schedule.examine.dto.response;

import com.booking_schedule.examine.enums.EStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialtyResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private EStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
