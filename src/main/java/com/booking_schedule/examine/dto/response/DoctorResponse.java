package com.booking_schedule.examine.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorResponse {
    // Định danh Doctor
    private Long id;
    private String doctorCode;
    private String status;

    // Thông tin từ User (Phẳng hóa)
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String phone;

    // Thông tin Chuyên khoa (Phẳng hóa)
    private Long specialtyId;
    private String specialtyName;

    // Thông tin chuyên môn Doctor
    private String position;
    private Integer experienceYears;
    private String description;

    // Auditing Info
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
