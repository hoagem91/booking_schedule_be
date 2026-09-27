package com.booking_schedule.examine.dto.response;
import com.booking_schedule.examine.enums.EDiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionResponse {
    private Long id;
    private String code;
    private String name;
    private EDiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private Integer usedCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
