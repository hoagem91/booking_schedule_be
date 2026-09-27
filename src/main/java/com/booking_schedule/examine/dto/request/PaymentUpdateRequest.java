package com.booking_schedule.examine.dto.request;
import com.booking_schedule.examine.enums.EPaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentUpdateRequest {

    @NotNull(message = "Trạng thái thanh toán không được để trống")
    private EPaymentStatus paymentStatus;

    // Mã giao dịch từ VNPay/Momo trả về (Nếu thanh toán tiền mặt thì để null)
    private String transactionCode;
}