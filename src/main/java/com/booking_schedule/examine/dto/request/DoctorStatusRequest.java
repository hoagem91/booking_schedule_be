package com.booking_schedule.examine.dto.request;
import com.booking_schedule.examine.enums.EStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DoctorStatusRequest {
    @NotNull(message = "DOCTOR_INVALID_REQUEST")
    private EStatus status;
}
