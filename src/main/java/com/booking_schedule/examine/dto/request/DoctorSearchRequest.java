package com.booking_schedule.examine.dto.request;
import com.booking_schedule.examine.enums.EStatus;
import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DoctorSearchRequest {
    private String keyword;
    @Positive(message = "DOCTOR_INVALID_REQUEST")
    private Long specialtyId;
    private EStatus status;
    @Pattern(regexp = "ACTIVE|INACTIVE", message = "DOCTOR_INVALID_REQUEST")
    private String userStatus;
}
