package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentCreateRequest {
    // Bắt buộc ở API admin; API patient lấy từ JWT và đối chiếu nếu client truyền.
    @Positive
    private Long patientId;

    @NotNull(message = "Bác sĩ (doctorId) không được để trống")
    @Positive
    private Long doctorId;

    @NotNull(message = "Dịch vụ khám (serviceId) không được để trống")
    @Positive
    private Long serviceId;

    @NotNull(message = "Khung giờ khám (timeSlotId) không được để trống")
    @Positive
    private Long timeSlotId;

    @NotNull(message = "Ngày khám không được để trống")
    @FutureOrPresent(message = "Ngày khám không được ở trong quá khứ")
    private LocalDate appointmentDate;

    @Size(max = 2000, message = "Lý do khám không vượt quá 2000 ký tự")
    private String reason;

    @Size(max = 2000, message = "Ghi chú không vượt quá 2000 ký tự")
    private String note;

    // Có thể null nếu bệnh nhân không có mã khuyến mãi
    @Positive
    private Long promotionId;
}
