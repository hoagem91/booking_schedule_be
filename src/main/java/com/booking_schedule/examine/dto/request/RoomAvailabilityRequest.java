package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomAvailabilityRequest {
    @NotNull(message = "ROOM_INVALID_REQUEST")
    @FutureOrPresent(message = "ROOM_INVALID_REQUEST")
    private LocalDate workDate;
    @NotNull(message = "ROOM_INVALID_REQUEST")
    private LocalTime startTime;
    @NotNull(message = "ROOM_INVALID_REQUEST")
    private LocalTime endTime;
    private String keyword;
    @Positive(message = "ROOM_INVALID_REQUEST")
    private Long specialtyId;
    private Boolean sharedOnly;
    @Positive(message = "ROOM_INVALID_REQUEST")
    private Long excludeScheduleId;

    @AssertTrue(message = "ROOM_INVALID_REQUEST")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null
                || (startTime.isBefore(endTime) && startTime.getNano() == 0 && endTime.getNano() == 0);
    }

    @AssertTrue(message = "ROOM_INVALID_REQUEST")
    public boolean isSpecialtySelectionValid() {
        return !Boolean.TRUE.equals(sharedOnly) || specialtyId == null;
    }
}
