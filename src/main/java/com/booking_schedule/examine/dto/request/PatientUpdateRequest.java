package com.booking_schedule.examine.dto.request;

import com.booking_schedule.examine.enums.EGender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientUpdateRequest {
    public void setAddress(String value) { address = value == null ? null : value.trim(); }
    public void setHealthInsuranceCode(String value) { healthInsuranceCode = value == null ? null : value.trim(); }
    private Boolean clearAddress;
    private Boolean clearHealthInsuranceCode;
    @AssertTrue(message = "PATIENT_INVALID_REQUEST")
    public boolean isClearSelectionValid() {
        return (!Boolean.TRUE.equals(clearAddress) || address == null)
                && (!Boolean.TRUE.equals(clearHealthInsuranceCode) || healthInsuranceCode == null);
    }
    @PastOrPresent(message = "Ngày sinh không được ở tương lai")
    private LocalDate dateOfBirth;

    private EGender gender;

    @Size(max = 500, message = "Địa chỉ không vượt quá 500 ký tự")
    @Pattern(regexp = "^.*\\S.*$", message = "Địa chỉ không được chỉ chứa khoảng trắng")
    private String address;

    @Size(max = 100, message = "Mã bảo hiểm y tế không vượt quá 100 ký tự")
    @Pattern(regexp = "^.*\\S.*$", message = "Mã bảo hiểm y tế không được chỉ chứa khoảng trắng")
    private String healthInsuranceCode;
}
