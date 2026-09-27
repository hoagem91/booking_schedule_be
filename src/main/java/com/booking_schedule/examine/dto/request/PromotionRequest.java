package com.booking_schedule.examine.dto.request;
import com.booking_schedule.examine.enums.EDiscountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionRequest {
    @NotBlank(message = "Mã khuyến mãi không được để trống")
    @Size(max = 50, message = "Mã khuyến mãi không vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên khuyến mãi không được để trống")
    @Size(max = 255, message = "Tên khuyến mãi không vượt quá 255 ký tự")
    private String name;

    @NotNull(message = "Loại giảm giá không được để trống")
    private EDiscountType discountType;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @PositiveOrZero(message = "Giá trị giảm giá phải lớn hơn hoặc bằng 0")
    private BigDecimal discountValue;

    @PositiveOrZero(message = "Mức giảm tối đa phải lớn hơn hoặc bằng 0")
    private BigDecimal maxDiscount;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Min(value = 1, message = "Giới hạn sử dụng phải lớn hơn 0")
    private Integer usageLimit;

    private String status;
}
