package com.booking_schedule.examine.dto.request;

import com.booking_schedule.examine.enums.EStatus;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorUpdateRequest {

    private Long specialtyId;

    // doctorCode có thể cập nhật nhưng phải unique — Service kiểm tra
    @Size(max = 50, message = "Mã bác sĩ không được vượt quá 50 ký tự")
    @Pattern(regexp = "^.*\\S.*$", message = "Mã bác sĩ không được chỉ chứa khoảng trắng")
    private String doctorCode;

    @Size(max = 100, message = "Chức vụ không vượt quá 100 ký tự")
    @Pattern(regexp = "^.*\\S.*$", message = "Chức vụ không được chỉ chứa khoảng trắng")
    private String position;

    @Min(value = 0, message = "Số năm kinh nghiệm phải lớn hơn hoặc bằng 0")
    @Max(value = 70, message = "Số năm kinh nghiệm không hợp lệ")
    private Integer experienceYears;

    @Size(max = 2000, message = "Tiểu sử/Mô tả không được vượt quá 2000 ký tự")
    private String description;

    // Admin có thể đổi status: ACTIVE | INACTIVE
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Trạng thái chỉ được là ACTIVE hoặc INACTIVE")
    private EStatus status;
}