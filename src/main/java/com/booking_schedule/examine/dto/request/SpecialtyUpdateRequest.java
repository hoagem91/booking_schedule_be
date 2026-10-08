package com.booking_schedule.examine.dto.request;

import com.booking_schedule.examine.enums.EStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialtyUpdateRequest {

    @Size(max = 50, message = "Mã chuyên khoa không được vượt quá 50 ký tự")
    @Pattern(regexp = "^.*\\S.*$", message = "Mã chuyên khoa không được chỉ chứa khoảng trắng")
    private String code;

    @Size(max = 255, message = "Tên chuyên khoa không được vượt quá 255 ký tự")
    @Pattern(regexp = "^.*\\S.*$", message = "Tên chuyên khoa không được chỉ chứa khoảng trắng")
    private String name;

    private String description;

    private EStatus status;
}
