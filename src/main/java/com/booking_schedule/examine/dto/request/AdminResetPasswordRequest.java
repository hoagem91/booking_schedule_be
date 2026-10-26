package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminResetPasswordRequest {
    @NotBlank(message = "INVALID_PASSWORD")
    @Size(min = 6, max = 100, message = "INVALID_PASSWORD")
    private String newPassword;
}
