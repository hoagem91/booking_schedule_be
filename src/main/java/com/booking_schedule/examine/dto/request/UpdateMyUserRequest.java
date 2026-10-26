package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
public class UpdateMyUserRequest {
    public void setEmail(String value) { this.email = value == null ? null : value.trim().toLowerCase(java.util.Locale.ROOT); }
    public void setFullName(String value) { this.fullName = value == null ? null : value.trim(); }
    public void setPhone(String value) { this.phone = value == null ? null : value.trim(); }
    private String currentPassword;
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
