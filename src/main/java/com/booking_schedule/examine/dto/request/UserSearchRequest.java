package com.booking_schedule.examine.dto.request;

import com.booking_schedule.examine.enums.ERole;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSearchRequest {
    private String keyword;
    @Pattern(regexp = "ACTIVE|INACTIVE", message = "USER_INVALID_REQUEST")
    private String status;
    private ERole role;
}
