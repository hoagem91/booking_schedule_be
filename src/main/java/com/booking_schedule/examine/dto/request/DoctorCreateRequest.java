package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorCreateRequest {

    @Positive(message = "DOCTOR_INVALID_REQUEST")
    private Long userId;

    @jakarta.validation.Valid
    private CreateUserRequest user;

    @AssertTrue(message = "DOCTOR_INVALID_REQUEST")
    public boolean isAccountSelectionValid() { return (userId == null) != (user == null); }

    @NotNull(message = "Chuyên khoa (specialtyId) không được để trống")
    private Long specialtyId;

    @Size(max = 50, message = "Mã bác sĩ không được vượt quá 50 ký tự")
    private String doctorCode;

    @NotBlank(message = "Chức vụ không được để trống")
    @Size(max = 100, message = "Chức vụ không vượt quá 100 ký tự")
    private String position;// Ví dụ: Bác sĩ CK I, PGS.TS, Trưởng khoa

    @NotNull(message = "Số năm kinh nghiệm không được để trống")
    @Min(value = 0, message = "Số năm kinh nghiệm phải lớn hơn hoặc bằng 0")
    @Max(value = 70, message = "Số năm kinh nghiệm không hợp lệ")
    private Integer experienceYears;

    @Size(max = 2000, message = "Tiểu sử/Mô tả không được vượt quá 2000 ký tự")
    private String description;
}
