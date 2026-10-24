package com.booking_schedule.examine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomStatusRequest {
    @NotBlank(message = "ROOM_INVALID_REQUEST")
    @Pattern(regexp = "AVAILABLE|OCCUPIED|MAINTENANCE|INACTIVE", message = "ROOM_INVALID_REQUEST")
    private String status;
}
