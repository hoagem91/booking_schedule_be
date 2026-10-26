package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChangeMyPasswordRequest {
    @NotBlank(message = "USER_CURRENT_PASSWORD_REQUIRED")
    private String currentPassword;
    @NotBlank(message = "INVALID_PASSWORD")
    @Size(min = 6, max = 100, message = "INVALID_PASSWORD")
    private String newPassword;
}
