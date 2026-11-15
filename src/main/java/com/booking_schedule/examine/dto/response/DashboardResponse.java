package com.booking_schedule.examine.dto.response;

import com.booking_schedule.examine.enums.EAppointmentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** Separate contracts for today's operations and date-filtered analytics. */
public final class DashboardResponse {
    private DashboardResponse() {}

    public record Summary(LocalDate date, String timezone, long appointmentsToday,
            long confirmedToday, long completedToday, BigDecimal collectedToday,
            long pendingAppointments, long reviewRequiredTransactions, long totalPatients,
            long workingDoctorsToday, long activeSpecialties, long activeServices,
            long successfulPaymentsMissingPaidAt) {}

    public record DailyAppointments(LocalDate date, long count) {}
    public record RankedItem(Long id, String name, long count) {}
    public record Analytics(LocalDate fromDate, LocalDate toDate, String timezone,
            long totalAppointments, long newlyCreatedAppointments,
            Map<EAppointmentStatus, Long> statusCounts, List<DailyAppointments> dailyAppointments,
            List<RankedItem> topSpecialties, List<RankedItem> topDoctors) {}

    public record DailyRevenue(LocalDate date, long successfulPayments, BigDecimal collectedAmount) {}
    public record MethodRevenue(String method, long successfulPayments, BigDecimal collectedAmount) {}
    public record Revenue(LocalDate fromDate, LocalDate toDate, String timezone, String currency,
            long successfulPayments, BigDecimal collectedAmount,
            List<DailyRevenue> dailyRevenue, List<MethodRevenue> byPaymentMethod,
            long successfulPaymentsMissingPaidAt) {}

    public record TodayAppointment(Long id, String appointmentCode, Long patientId, String patientName,
            Long doctorId, String doctorName, Long specialtyId, String specialtyName,
            String serviceName, LocalDate appointmentDate, LocalTime startTime, LocalTime endTime,
            EAppointmentStatus status) {}
}
