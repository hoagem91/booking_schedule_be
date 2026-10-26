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
    private Long doctorId;
    private Long patientId;
    private Capabilities capabilities;
    public record Capabilities(boolean self, boolean lastActiveSuperAdmin, boolean canEdit,
            boolean canChangeStatus, boolean canChangeRoles, boolean canResetPassword, boolean canDeactivate) {}
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
