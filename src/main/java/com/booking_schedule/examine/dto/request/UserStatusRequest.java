package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserStatusRequest {
    @NotBlank(message = "USER_INVALID_REQUEST")
    @Pattern(regexp = "ACTIVE|INACTIVE", message = "USER_INVALID_REQUEST")
    private String status;
}
