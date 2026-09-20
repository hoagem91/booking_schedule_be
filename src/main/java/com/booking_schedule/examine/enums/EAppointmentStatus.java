package com.booking_schedule.examine.enums;

public enum EAppointmentStatus {
    PENDING,      // Mới đặt, chờ xác nhận
    CONFIRMED,    // Đã xác nhận
    IN_PROGRESS,  // Đang khám
    COMPLETED,    // Đã khám xong
    CANCELLED,    // Đã hủy
    NO_SHOW       // Bệnh nhân không đến
}
