package com.booking_schedule.examine.dto.response;
import com.booking_schedule.examine.enums.EPaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private String paymentCode;

    // Phẳng hóa thông tin Cuộc hẹn
    private Long appointmentId;
    private String appointmentCode;

    // Phẳng hóa thông tin Bệnh nhân (hiển thị trên biên lai)
    private String patientCode;
    private String patientName;
    private String patientPhone;

    // Thông tin thanh toán chính
    private BigDecimal amount;
    private String paymentMethod;
    private EPaymentStatus paymentStatus;
    private String transactionCode;
    private LocalDateTime paidAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
