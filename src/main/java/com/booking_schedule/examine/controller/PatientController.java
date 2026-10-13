package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.PatientCreateRequest;
import com.booking_schedule.examine.dto.request.PatientUpdateRequest;
import com.booking_schedule.examine.dto.response.ApiResponse;
import com.booking_schedule.examine.dto.response.PatientResponse;
import com.booking_schedule.examine.service.PatientService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PatientController {

    PatientService patientService;

    /**
     * POST /patients
     * User đang đăng nhập tự tạo hồ sơ bệnh nhân cho chính mình.
     * userId được lấy từ JWT token (SecurityContext), không truyền từ body.
     */
    @PostMapping
    public ApiResponse<PatientResponse> createPatient(@RequestBody @Valid PatientCreateRequest request) {
        return ApiResponse.<PatientResponse>builder()
                .result(patientService.createPatient(request))
                .build();
    }

    /**
     * GET /patients/me
     * User đang đăng nhập xem hồ sơ bệnh nhân của chính mình.
     */
    @GetMapping("/me")
    public ApiResponse<PatientResponse> getMyPatient() {
        return ApiResponse.<PatientResponse>builder()
                .result(patientService.getMyPatient())
                .build();
    }

    /**
     * PUT /patients/me
     * User đang đăng nhập cập nhật hồ sơ bệnh nhân của chính mình.
     */
    @PutMapping("/me")
    public ApiResponse<PatientResponse> updateMyPatient(@RequestBody @Valid PatientUpdateRequest request) {
        return ApiResponse.<PatientResponse>builder()
                .result(patientService.updateMyPatient(request))
                .build();
    }

    /**
     * GET /patients/{userId}   (Admin)
     * Admin xem hồ sơ bệnh nhân theo userId.
     */
    @GetMapping("/{userId}")
    public ApiResponse<PatientResponse> getPatientByUserId(@PathVariable Long userId) {
        return ApiResponse.<PatientResponse>builder()
                .result(patientService.getPatientByUserId(userId))
                .build();
    }

    /**
     * PUT /patients/{userId}   (Admin)
     * Admin cập nhật hồ sơ bệnh nhân theo userId.
     */
    @PutMapping("/{userId}")
    public ApiResponse<PatientResponse> updatePatientByUserId(
            @PathVariable Long userId,
            @RequestBody @Valid PatientUpdateRequest request) {
        return ApiResponse.<PatientResponse>builder()
                .result(patientService.updatePatientByUserId(userId, request))
                .build();
    }

    /**
     * DELETE /patients/{userId}   (Admin)
     * Endpoint cũ trả 409: hồ sơ được giữ lại; khóa User để ngừng tài khoản.
     */
    @DeleteMapping("/{userId}")
    public ApiResponse<String> deletePatient(@PathVariable Long userId) {
        patientService.deletePatient(userId);
        return ApiResponse.<String>builder()
                .result("Patient profile has been deleted")
                .build();
    }

    /**
     * GET /patients/all   (Admin)
     * Admin lấy toàn bộ danh sách hồ sơ bệnh nhân.
     */
    @GetMapping("/all")
    public ApiResponse<List<PatientResponse>> getAllPatients() {
        return ApiResponse.<List<PatientResponse>>builder()
                .result(patientService.getAllPatient())
                .build();
    }
}
