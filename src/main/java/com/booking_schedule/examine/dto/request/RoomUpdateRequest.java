package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomUpdateRequest {
    @Size(max = 50, message = "ROOM_INVALID_REQUEST")
    @Pattern(regexp = "(?s).*\\S.*", message = "ROOM_INVALID_REQUEST")
    private String code;
    @Size(max = 255, message = "ROOM_INVALID_REQUEST")
    @Pattern(regexp = "(?s).*\\S.*", message = "ROOM_INVALID_REQUEST")
    private String name;
    @Size(max = 255, message = "ROOM_INVALID_REQUEST")
    private String location;
    @Positive(message = "ROOM_INVALID_REQUEST")
    private Long specialtyId;
    private Boolean clearLocation;
    private Boolean clearSpecialty;
    @Pattern(regexp = "AVAILABLE|OCCUPIED|MAINTENANCE|INACTIVE", message = "ROOM_INVALID_REQUEST")
    private String status;

    @AssertTrue(message = "ROOM_INVALID_REQUEST")
    public boolean isSelectionValid() {
        return (!Boolean.TRUE.equals(clearLocation) || location == null)
                && (!Boolean.TRUE.equals(clearSpecialty) || specialtyId == null);
    }
}
