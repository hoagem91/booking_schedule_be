package com.booking_schedule.examine.dto.request;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AdminPatientUpdateRequest extends PatientUpdateRequest {
    @Valid
    private PatientAccountUpdateRequest account;
}
