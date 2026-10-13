package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
public class UpdateMyUserRequest {
    @Size(min = 6, max = 100)
    @Pattern(regexp = "^.*\\S.*$")
    private String password;
    @Size(max = 100)
    @Pattern(regexp = "^.*\\S.*$")
    private String fullName;
    @Email
    @Pattern(regexp = "^.*\\S.*$")
    private String email;
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$")
    private String phone;
}
