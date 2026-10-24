package com.booking_schedule.examine.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceResponse {
    private Long id;
    private String code;
    private String name;
    private Long specialtyId;
    private String specialtyName;
    private BigDecimal price;
    private Integer durationMinutes;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
