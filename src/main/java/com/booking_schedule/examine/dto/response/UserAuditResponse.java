package com.booking_schedule.examine.dto.response;

import java.time.LocalDateTime;

public record UserAuditResponse(Long id, Long actorUserId, Long targetUserId, String action,
        boolean passwordChanged, String beforeState, String afterState, LocalDateTime createdAt) {}
