package com.booking_schedule.examine.dto.response;
import com.booking_schedule.examine.enums.ESettingKey;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingResponse {
    private Long id;
    private ESettingKey settingKey;
    private String settingValue;
    private String description;
    private LocalDateTime updatedAt;
}
