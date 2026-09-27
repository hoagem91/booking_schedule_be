package com.booking_schedule.examine.dto.response;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {

    private Long id;
    private String code;
    private String name;
    private String location;

    // Phẳng hóa thông tin Specialty
    private Long specialtyId;
    private String specialtyName;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
