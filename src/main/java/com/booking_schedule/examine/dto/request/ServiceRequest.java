package com.booking_schedule.examine.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequest {

    @NotBlank(message = "Mã dịch vụ không được để trống")
    @Size(max = 50, message = "Mã dịch vụ không vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên dịch vụ không được để trống")
    @Size(max = 255, message = "Tên dịch vụ không vượt quá 255 ký tự")
    private String name;

    @NotNull(message = "ID chuyên khoa không được để trống")
    private Long specialtyId;

    @NotNull(message = "Giá dịch vụ không được để trống")
    @PositiveOrZero(message = "Giá dịch vụ phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @Min(value = 1, message = "Thời lượng khám tối thiểu là 1 phút")
    private Integer durationMinutes;

    private String description;

    @Size(max = 30, message = "Trạng thái không vượt quá 30 ký tự")
    private String status;
}