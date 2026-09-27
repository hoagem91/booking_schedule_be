package com.booking_schedule.examine.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorUpdateRequest {

    @NotNull(message = "Chuyên khoa không được để trống")
    private Long specialtyId;

    @NotBlank(message = "Chức vụ không được để trống")
    @Size(max = 100, message = "Chức vụ không vượt quá 100 ký tự")
    private String position;

    @NotNull(message = "Số năm kinh nghiệm không được để trống")
    @Min(value = 0, message = "Số năm kinh nghiệm phải lớn hơn hoặc bằng 0")
    private Integer experienceYears;

    @Size(max = 2000, message = "Tiểu sử/Mô tả không được vượt quá 2000 ký tự")
    private String description;
}