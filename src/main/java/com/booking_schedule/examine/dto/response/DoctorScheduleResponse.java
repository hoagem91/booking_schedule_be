package com.booking_schedule.examine.dto.response;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorScheduleResponse {

    private Long id;

    // Phẳng hóa thông tin Bác sĩ
    private Long doctorId;
    private String doctorCode;
    private String doctorName;
    private String specialtyName;

    // Phẳng hóa thông tin Phòng khám
    private Long roomId;
    private String roomCode;
    private String roomName;

    // Thông tin tổng quan của Lịch
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxPatients;
    private String status;

    // Danh sách các khung giờ chi tiết (TimeSlot) bên trong lịch này
    private List<TimeSlotResponse> timeSlots;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
