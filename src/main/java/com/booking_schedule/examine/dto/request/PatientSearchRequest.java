package com.booking_schedule.examine.dto.request;

import com.booking_schedule.examine.enums.EGender;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PatientSearchRequest {
    private String keyword;
    private EGender gender;
    @Pattern(regexp = "ACTIVE|INACTIVE", message = "PATIENT_INVALID_REQUEST")
    private String userStatus;
    @PastOrPresent(message = "PATIENT_INVALID_REQUEST")
    private LocalDate dateOfBirthFrom;
    @PastOrPresent(message = "PATIENT_INVALID_REQUEST")
    private LocalDate dateOfBirthTo;
    @AssertTrue(message = "PATIENT_INVALID_REQUEST")
    public boolean isDateRangeValid() {
        return dateOfBirthFrom == null || dateOfBirthTo == null || !dateOfBirthFrom.isAfter(dateOfBirthTo);
    }
}
