package com.booking_schedule.examine.dto.request;
import jakarta.validation.constraints.*;
import com.booking_schedule.examine.enums.EStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceUpdateRequest {

    @Size(max = 50, message = "Mã dịch vụ không vượt quá 50 ký tự")
    @Pattern(regexp = "(?s).*\\S.*", message = "Mã dịch vụ không được để trống")
    private String code;

    @Size(max = 255, message = "Tên dịch vụ không vượt quá 255 ký tự")
    @Pattern(regexp = "(?s).*\\S.*", message = "Tên dịch vụ không được để trống")
    private String name;

    @Positive(message = "ID chuyên khoa phải lớn hơn 0")
    private Long specialtyId;

    private Boolean clearSpecialty;

    @AssertTrue(message = "SERVICE_INVALID_REQUEST")
    public boolean isSpecialtySelectionValid() {
        return !Boolean.TRUE.equals(clearSpecialty) || specialtyId == null;
    }

    @PositiveOrZero(message = "Giá dịch vụ phải lớn hơn hoặc bằng 0")
    @Digits(integer = 13, fraction = 2, message = "Giá dịch vụ tối đa 13 chữ số nguyên và 2 chữ số thập phân")
    private BigDecimal price;

    @Min(value = 1, message = "Thời lượng khám tối thiểu là 1 phút")
    private Integer durationMinutes;

    private String description;

    private EStatus status;
}
