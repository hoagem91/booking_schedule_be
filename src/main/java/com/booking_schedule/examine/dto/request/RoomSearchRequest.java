package com.booking_schedule.examine.dto.request;

import lombok.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomSearchRequest {
    private String keyword;
    @Positive(message = "ROOM_INVALID_REQUEST")
    private Long specialtyId;
    private String status;
    private Boolean sharedOnly;

    @AssertTrue(message = "ROOM_INVALID_REQUEST")
    public boolean isSpecialtySelectionValid() {
        return !Boolean.TRUE.equals(sharedOnly) || specialtyId == null;
    }
}
