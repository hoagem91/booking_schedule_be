package com.booking_schedule.examine.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PatientAuditResponse(Long id, Long actorUserId, Long patientId, String action,
        List<String> changedFields, LocalDateTime createdAt) {}
