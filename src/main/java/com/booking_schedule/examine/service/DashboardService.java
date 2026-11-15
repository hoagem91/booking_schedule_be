package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.response.DashboardResponse.*;
import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.repository.DashboardRepository;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
public class DashboardService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final DashboardRepository repository;

    public Summary summary() { return repository.summary(LocalDate.now(ZONE)); }

    public Analytics analytics(LocalDate from, LocalDate to) {
        var range = range(from, to);
        var counts = repository.statusCounts(range.from, range.to);
        var daily = new HashMap<LocalDate, DailyAppointments>();
        repository.dailyAppointments(range.from, range.to).forEach(row -> daily.put(row.date(), row));
        var filled = range.from.datesUntil(range.to.plusDays(1))
                .map(day -> daily.getOrDefault(day, new DailyAppointments(day, 0))).toList();
        return new Analytics(range.from, range.to, ZONE.getId(), counts.values().stream().mapToLong(Long::longValue).sum(),
                repository.newlyCreated(range.from, range.to), counts, filled,
                repository.top(range.from, range.to, false), repository.top(range.from, range.to, true));
    }

    public Revenue revenue(LocalDate from, LocalDate to) {
        var range = range(from, to);
        var daily = new HashMap<LocalDate, DailyRevenue>();
        repository.dailyRevenue(range.from, range.to).forEach(row -> daily.put(row.date(), row));
        var filled = range.from.datesUntil(range.to.plusDays(1))
                .map(day -> daily.getOrDefault(day, new DailyRevenue(day, 0, BigDecimal.ZERO))).toList();
        return new Revenue(range.from, range.to, ZONE.getId(), "VND",
                filled.stream().mapToLong(DailyRevenue::successfulPayments).sum(),
                filled.stream().map(DailyRevenue::collectedAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                filled, repository.byMethod(range.from, range.to), repository.missingPaidAt());
    }

    public Page<TodayAppointment> todayAppointments(boolean upcomingOnly, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) invalid();
        return repository.todayAppointments(LocalDateTime.now(ZONE), upcomingOnly, PageRequest.of(page, size));
    }

    private Range range(LocalDate from, LocalDate to) {
        if (to == null) to = LocalDate.now(ZONE);
        if (to.getYear() < 1900 || to.getYear() > 9999) invalid();
        if (from == null) from = to.minusDays(29);
        if (from.getYear() < 1900 || to.getYear() > 9999 || from.isAfter(to)
                || ChronoUnit.DAYS.between(from, to) >= 366) invalid();
        return new Range(from, to);
    }

    private void invalid() { throw new AppException(ErrorCode.DASHBOARD_INVALID_REQUEST); }
    private record Range(LocalDate from, LocalDate to) {}
}
