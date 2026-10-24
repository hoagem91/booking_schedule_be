package com.booking_schedule.examine.dto.response;

import com.booking_schedule.examine.enums.EAppointmentStatus;
import java.util.List;
import java.util.Map;

public record AppointmentSummaryResponse(long total, Map<EAppointmentStatus, Long> statusCounts,
        List<DoctorSummary> doctors) {
    public record DoctorSummary(Long doctorId, String doctorCode, String doctorName,
            long total, Map<EAppointmentStatus, Long> statusCounts) {}
}
