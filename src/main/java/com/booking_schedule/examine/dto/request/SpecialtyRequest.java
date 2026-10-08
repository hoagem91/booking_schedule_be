package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialtyRequest {
    // Không bắt buộc - Service tự sinh nếu không truyền (VD: CK-NHKHOA)
    @Size(max = 50, message = "Mã chuyên khoa không được vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên chuyên khoa không được để trống")
    @Size(max = 255, message = "Tên chuyên khoa không được vượt quá 255 ký tự")
    private String name;

    private String description;
}
