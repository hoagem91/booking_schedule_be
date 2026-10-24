package com.booking_schedule.examine.dto.request;

import com.booking_schedule.examine.enums.EStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceStatusRequest {
    @NotNull(message = "SERVICE_INVALID_REQUEST")
    private EStatus status;
}
