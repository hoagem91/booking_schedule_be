package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.SpecialtyRequest;
import com.booking_schedule.examine.dto.request.SpecialtyUpdateRequest;
import com.booking_schedule.examine.dto.request.SpecialtySearchRequest;
import com.booking_schedule.examine.dto.request.SpecialtyStatusRequest;
import com.booking_schedule.examine.dto.response.SpecialtyDeleteResponse;
import com.booking_schedule.examine.dto.response.ApiResponse;
import com.booking_schedule.examine.dto.response.SpecialtyResponse;
import com.booking_schedule.examine.enums.EStatus;
import com.booking_schedule.examine.service.SpecialtyService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;

import java.util.List;

@RestController
@RequestMapping({"/api/admin/specialties", "/admin/specialties"})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SpecialtyController {
    SpecialtyService specialtyService;

    @PostMapping
    public ApiResponse<SpecialtyResponse> createSpecialty(@RequestBody @Valid SpecialtyRequest request) {
        return ApiResponse.<SpecialtyResponse>builder()
                .result(specialtyService.createSpecialty(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<SpecialtyResponse> updateSpecialty(@PathVariable Long id, @RequestBody @Valid SpecialtyUpdateRequest request) {
        return ApiResponse.<SpecialtyResponse>builder()
                .result(specialtyService.updateSpecialty(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<SpecialtyDeleteResponse> deleteSpecialty(@PathVariable Long id) {
        return ApiResponse.<SpecialtyDeleteResponse>builder()
                .result(specialtyService.deleteSpecialty(id))
                .build();
    }

    @PostMapping("/search")
    public ApiResponse<Page<SpecialtyResponse>> search(@Valid @RequestBody SpecialtySearchRequest request,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.<Page<SpecialtyResponse>>builder()
                .result(specialtyService.search(request.getKeyword(), request.getStatus(), pageable)).build();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<SpecialtyResponse> status(@PathVariable Long id, @Valid @RequestBody SpecialtyStatusRequest request) {
        return ApiResponse.<SpecialtyResponse>builder()
                .result(specialtyService.updateStatus(id, request.getStatus())).build();
    }

    @GetMapping
    public ApiResponse<List<SpecialtyResponse>> getAllSpecialties(
            @RequestParam(required = false) EStatus status) {
        return ApiResponse.<List<SpecialtyResponse>>builder()
                .result(specialtyService.getAllSpecialties(status))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<SpecialtyResponse> getSpecialtyById(@PathVariable Long id) {
        return ApiResponse.<SpecialtyResponse>builder()
                .result(specialtyService.getSpecialtyById(id))
                .build();
    }
}
