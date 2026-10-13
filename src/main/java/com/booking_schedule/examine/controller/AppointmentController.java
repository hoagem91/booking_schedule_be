package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.*;
import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.service.AppointmentService;
import com.booking_schedule.examine.service.AppointmentReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/patient/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService service;
    private final AppointmentReviewService reviewService;

    @PostMapping
    public ApiResponse<AppointmentResponse> create(@Valid @RequestBody AppointmentCreateRequest request) {
        return wrap(service.createAppointment(request));
    }
    @GetMapping("/my-history")
    public ApiResponse<Page<AppointmentResponse>> history(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<Page<AppointmentResponse>>builder().result(service.history(pageable)).build();
    }
    @GetMapping("/{id}")
    public ApiResponse<AppointmentResponse> get(@PathVariable Long id) { return wrap(service.getMine(id)); }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<AppointmentResponse> cancel(@PathVariable Long id, @Valid @RequestBody(required = false) AppointmentActionRequest request) {
        return wrap(service.cancelMine(id, request == null ? null : request.getNote()));
    }
    @PostMapping("/{id}/review")
    public ApiResponse<ReviewResponse> review(@PathVariable Long id, @Valid @RequestBody AppointmentReviewRequest request) {
        return ApiResponse.<ReviewResponse>builder().result(reviewService.create(id, request)).build();
    }
    private ApiResponse<AppointmentResponse> wrap(AppointmentResponse result) {
        return ApiResponse.<AppointmentResponse>builder().result(result).build();
    }
}
