package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.AnalyticsRequest;
import com.booking_schedule.examine.dto.response.ApiResponse;
import com.booking_schedule.examine.dto.response.DashboardResponse.*;
import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final DashboardService service;

    @GetMapping("/summary")
    public ApiResponse<Summary> summary() {
        return ApiResponse.<Summary>builder().result(service.summary()).build();
    }

    @PostMapping("/analytics")
    public ApiResponse<Analytics> analytics(
            @RequestBody AnalyticsRequest request) {
        return ApiResponse.<Analytics>builder().result(service.analytics(request.getFromDate(), request.getToDate())).build();
    }

    @PostMapping("/revenue")
    public ApiResponse<Revenue> revenue(
            @RequestBody AnalyticsRequest request) {
        return ApiResponse.<Revenue>builder().result(service.revenue(request.getFromDate(), request.getToDate())).build();
    }

    @GetMapping("/today-appointments")
    public ApiResponse<Page<TodayAppointment>> todayAppointments(
            @RequestParam(defaultValue = "false") boolean upcomingOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<Page<TodayAppointment>>builder().result(service.todayAppointments(upcomingOnly, page, size)).build();
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public org.springframework.http.ResponseEntity<ApiResponse<Void>> invalidParameter() {
        var error = ErrorCode.DASHBOARD_INVALID_REQUEST;
        return org.springframework.http.ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                .code(error.getCode()).message(error.getMessage()).build());
    }
}
