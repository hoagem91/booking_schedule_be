package com.booking_schedule.examine.dto.response;
import com.booking_schedule.examine.enums.EAppointmentStatus;
import com.booking_schedule.examine.enums.EDiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {
    private Long id;
    private String appointmentCode;

    // Phẳng hóa thông tin Bệnh nhân (Từ Patient & User)
    private Long patientId;
    private String patientCode;
    private String patientName;
    private String patientPhone;

    // Phẳng hóa thông tin Bác sĩ (Từ Doctor & User)
    private Long doctorId;
    private String doctorCode;
    private String doctorName;

    // Phẳng hóa thông tin Dịch vụ khám
    private Long serviceId;
    private String serviceName;
    private BigDecimal servicePrice;
    private BigDecimal finalAmount;
    private boolean reviewEligible;

    // Phẳng hóa thông tin Khung giờ (Từ TimeSlot)
    private Long timeSlotId;
    private LocalTime startTime;
    private LocalTime endTime;

    // Thông tin Khuyến mãi (Nếu có)
    private Long promotionId;
    private String promotionCode;
    private String promotionName;
    private EDiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscount;

    // Thông tin chính của Appointment
    private LocalDate appointmentDate;
    private String reason;
    private EAppointmentStatus status;
    private String note;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
