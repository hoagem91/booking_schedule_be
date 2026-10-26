package com.booking_schedule.examine.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AdminPatientCreateRequest extends PatientCreateRequest {
    @Positive(message = "PATIENT_INVALID_REQUEST")
    private Long userId;
    @Valid
    private CreateUserRequest user;
    @AssertTrue(message = "PATIENT_INVALID_REQUEST")
    public boolean isAccountSelectionValid() { return (userId == null) != (user == null); }
}
