package com.booking_schedule.examine.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomSearchRequest {
    private String keyword;
    private Long specialtyId;
    private String status;
}
