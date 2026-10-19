package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.*;
import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.service.DoctorScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/doctor-schedules")
@RequiredArgsConstructor
public class DoctorScheduleController {
    private final DoctorScheduleService service;

    @PostMapping
    public ApiResponse<DoctorScheduleResponse> create(@Valid @RequestBody DoctorScheduleCreateRequest request) {
        return ApiResponse.<DoctorScheduleResponse>builder().result(service.createDoctorSchedule(request)).build();
    }
    @PutMapping("/{id}")
    public ApiResponse<DoctorScheduleResponse> update(@PathVariable Long id, @Valid @RequestBody DoctorScheduleUpdateRequest request) {
        return ApiResponse.<DoctorScheduleResponse>builder().result(service.updateDoctorSchedule(id, request)).build();
    }
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        return ApiResponse.<String>builder().result(service.deleteDoctorSchedule(id)).build();
    }
    @PatchMapping("/{id}/status")
    public ApiResponse<DoctorScheduleResponse> status(@PathVariable Long id, @Valid @RequestBody DoctorScheduleStatusRequest request) {
        return ApiResponse.<DoctorScheduleResponse>builder().result(service.changeStatus(id, request.getStatus())).build();
    }
    @GetMapping("/{id}")
    public ApiResponse<DoctorScheduleResponse> get(@PathVariable Long id) {
        return ApiResponse.<DoctorScheduleResponse>builder().result(service.getDoctorSchedule(id)).build();
    }
    @PostMapping("/search")
    public ApiResponse<Page<DoctorScheduleResponse>> search(@Valid @RequestBody DoctorScheduleSearchRequest request,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.<Page<DoctorScheduleResponse>>builder()
                .result(service.search(request.getDoctorId(), request.getSpecialtyId(), request.getFromDate(),
                        request.getToDate(), request.getStatus(), pageable)).build();
    }
}
