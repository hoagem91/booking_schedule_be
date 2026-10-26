package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter @Setter
public class AdminCreateUserRequest extends CreateUserRequest {
    @Size(min = 1, message = "INVALID_ROLE")
    private Set<String> roles;
}
