package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.*;
import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.enums.EAppointmentStatus;
import com.booking_schedule.examine.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/appointments")
@RequiredArgsConstructor
public class AdminAppointmentController {
    private final AppointmentService service;

    @PostMapping
    public ApiResponse<AppointmentResponse> create(@Valid @RequestBody AppointmentCreateRequest request) {
        return wrap(service.createForPatient(request));
    }
    @GetMapping("/{id}")
    public ApiResponse<AppointmentResponse> get(@PathVariable Long id) { return wrap(service.get(id)); }

    @PostMapping("/search")
    public ApiResponse<Page<AppointmentResponse>> search(@Valid @RequestBody AppointmentSearchRequest request,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<Page<AppointmentResponse>>builder()
                .result(service.search(request.getKeyword(), request.getDoctorId(), request.getPatientId(),
                        request.getStatus(), request.getFromDate(), request.getToDate(), pageable)).build();
    }
    @PostMapping("/summary")
    public ApiResponse<AppointmentSummaryResponse> summary(@Valid @RequestBody AppointmentSearchRequest request) {
        return ApiResponse.<AppointmentSummaryResponse>builder().result(service.summary(request)).build();
    }

    @PatchMapping("/{id}/confirm")
    public ApiResponse<AppointmentResponse> confirm(@PathVariable Long id, @Valid @RequestBody(required = false) AppointmentActionRequest request) {
        return action(id, EAppointmentStatus.CONFIRMED, request);
    }
    @PatchMapping("/{id}/in-progress")
    public ApiResponse<AppointmentResponse> start(@PathVariable Long id, @Valid @RequestBody(required = false) AppointmentActionRequest request) {
        return action(id, EAppointmentStatus.IN_PROGRESS, request);
    }
    @PatchMapping("/{id}/complete")
    public ApiResponse<AppointmentResponse> complete(@PathVariable Long id, @Valid @RequestBody(required = false) AppointmentActionRequest request) {
        return action(id, EAppointmentStatus.COMPLETED, request);
    }
    @PatchMapping("/{id}/cancel")
    public ApiResponse<AppointmentResponse> cancel(@PathVariable Long id, @Valid @RequestBody(required = false) AppointmentActionRequest request) {
        return action(id, EAppointmentStatus.CANCELLED, request);
    }
    @PatchMapping("/{id}/no-show")
    public ApiResponse<AppointmentResponse> noShow(@PathVariable Long id, @Valid @RequestBody(required = false) AppointmentActionRequest request) {
        return action(id, EAppointmentStatus.NO_SHOW, request);
    }
    private ApiResponse<AppointmentResponse> action(Long id, EAppointmentStatus target, AppointmentActionRequest request) {
        return wrap(service.transition(id, target, request == null ? null : request.getNote()));
    }
    private ApiResponse<AppointmentResponse> wrap(AppointmentResponse result) {
        return ApiResponse.<AppointmentResponse>builder().result(result).build();
    }
}
