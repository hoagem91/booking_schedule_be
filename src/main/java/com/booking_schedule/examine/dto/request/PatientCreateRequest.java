package com.booking_schedule.examine.dto.request;

import com.booking_schedule.examine.enums.EGender;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientCreateRequest {
    // Có thể truyền lên hoặc để null cho Service tự sinh mã (VD: BN-20260927-001)
    @Size(max = 50, message = "Mã bệnh nhân không được vượt quá 50 ký tự")
    private String patientCode;

    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
    private LocalDate dateOfBirth;

    @NotNull(message = "Giới tính không được để trống")
    private EGender gender;

    @Size(max = 500, message = "Địa chỉ không vượt quá 500 ký tự")
    private String address;

    @Size(max = 100, message = "Mã bảo hiểm y tế không vượt quá 100 ký tự")
    private String healthInsuranceCode;
}
