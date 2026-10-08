package com.booking_schedule.examine.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String status;
    private Set<RoleResponse> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
