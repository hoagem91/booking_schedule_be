package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.*;
import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/patients")
@RequiredArgsConstructor
public class AdminPatientController {
    private final PatientService service;
    @PostMapping
    public ApiResponse<PatientResponse> create(@Valid @RequestBody AdminPatientCreateRequest request) {
        return wrap(service.createAdminPatient(request));
    }
    @PostMapping("/search")
    public ApiResponse<Page<PatientResponse>> search(@Valid @RequestBody PatientSearchRequest request,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.<Page<PatientResponse>>builder().result(service.search(request, pageable)).build();
    }
    @GetMapping("/{patientId}")
    public ApiResponse<PatientResponse> get(@PathVariable Long patientId) { return wrap(service.getAdminPatient(patientId)); }
    @PutMapping("/{patientId}")
    public ApiResponse<PatientResponse> update(@PathVariable Long patientId, @Valid @RequestBody AdminPatientUpdateRequest request) {
        return wrap(service.updateAdminPatient(patientId, request));
    }
    @GetMapping("/{patientId}/audit-logs")
    public ApiResponse<Page<PatientAuditResponse>> audit(@PathVariable Long patientId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<Page<PatientAuditResponse>>builder().result(service.auditLogs(patientId, pageable)).build();
    }
    @DeleteMapping("/{patientId}")
    public ApiResponse<Void> delete(@PathVariable Long patientId) {
        service.deleteAdminPatient(patientId);
        return ApiResponse.<Void>builder().build();
    }
    private ApiResponse<PatientResponse> wrap(PatientResponse response) {
        return ApiResponse.<PatientResponse>builder().result(response).build();
    }
}
