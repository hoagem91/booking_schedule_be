package com.booking_schedule.examine.dto.request;

import com.booking_schedule.examine.enums.EGender;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest extends CreateUserRequest {
    public void setAddress(String value) { address = value == null ? null : value.trim(); }
    public void setHealthInsuranceCode(String value) { healthInsuranceCode = value == null ? null : value.trim(); }
    @NotNull(message = "Ngày sinh không được để trống")
    @PastOrPresent(message = "Ngày sinh không được ở tương lai")
    private LocalDate dateOfBirth;
    @NotNull(message = "Giới tính không được để trống")
    private EGender gender;
    @Size(max = 500)
    private String address;
    @Size(max = 100)
    private String healthInsuranceCode;
}
