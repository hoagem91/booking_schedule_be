package com.booking_schedule.examine.dto.response;
import java.time.LocalDateTime;
import java.util.List;
public record DoctorAuditResponse(Long id, Long actorUserId, Long doctorId, String action,
                                  List<String> changedFields, LocalDateTime createdAt) {}
