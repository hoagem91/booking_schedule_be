package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRolesRequest {
    @NotEmpty(message = "INVALID_ROLE")
    private Set<@NotBlank(message = "INVALID_ROLE") String> roles;
}
