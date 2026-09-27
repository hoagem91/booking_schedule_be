package com.booking_schedule.examine.dto.request;
import com.booking_schedule.examine.enums.ESettingKey;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingRequest {
    @NotNull(message = "Khóa cấu hình (settingKey) không được để trống")
    private ESettingKey settingKey;

    private String settingValue;

    @Size(max = 500, message = "Mô tả không vượt quá 500 ký tự")
    private String description;
}
