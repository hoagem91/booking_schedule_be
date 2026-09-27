package com.booking_schedule.examine.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreateRequest {
    @NotNull(message = "ID cuộc hẹn (appointmentId) không được để trống")
    private Long appointmentId;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    @Size(max = 30, message = "Phương thức thanh toán không vượt quá 30 ký tự")
    private String paymentMethod;
}
