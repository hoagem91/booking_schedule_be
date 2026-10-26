package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.Locale;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PatientAccountUpdateRequest {
    @Size(max = 100, message = "USER_INVALID_REQUEST")
    @Pattern(regexp = "(?s).*\\S.*", message = "USER_INVALID_REQUEST")
    private String fullName;
    @Email(message = "INVALID_EMAIL")
    @Pattern(regexp = "(?s).*\\S.*", message = "INVALID_EMAIL")
    private String email;
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "USER_INVALID_REQUEST")
    private String phone;
    public void setFullName(String value) { fullName = value == null ? null : value.trim(); }
    public void setEmail(String value) { email = value == null ? null : value.trim().toLowerCase(Locale.ROOT); }
    public void setPhone(String value) { phone = value == null ? null : value.trim(); }
}
