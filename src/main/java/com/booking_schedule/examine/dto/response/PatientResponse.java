package com.booking_schedule.examine.dto.response;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponse {
    // Thông tin định danh Patient
    private Long id;
    private String patientCode;

    // Thông tin cá nhân lấy từ User (Phẳng hóa)
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String phone;

    // Thông tin chi tiết của Patient
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String healthInsuranceCode;

    // Auditing Info
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
