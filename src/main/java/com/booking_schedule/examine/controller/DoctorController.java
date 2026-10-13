package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.DoctorCreateRequest;
import com.booking_schedule.examine.dto.request.DoctorUpdateRequest;
import com.booking_schedule.examine.dto.response.ApiResponse;
import com.booking_schedule.examine.dto.response.DoctorResponse;
import com.booking_schedule.examine.enums.EStatus;
import com.booking_schedule.examine.service.DoctorService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/doctors")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DoctorController {

    DoctorService doctorService;

    /**
     * POST /api/admin/doctors
     * Admin tạo hồ sơ bác sĩ và gán role DOCTOR cùng transaction.
     * userId truyền qua body request (Admin chỉ định user nào).
     */
    @PostMapping
    public ApiResponse<DoctorResponse> createDoctor(@RequestBody @Valid DoctorCreateRequest request) {
        return ApiResponse.<DoctorResponse>builder()
                .result(doctorService.createDoctor(request))
                .build();
    }

    /**
     * GET /api/admin/doctors
     * Lấy danh sách bác sĩ, hỗ trợ lọc theo specialtyId và/hoặc status.
     * Query params (tùy chọn):
     *   - specialtyId: Long
     *   - status: "ACTIVE" | "INACTIVE"
     */
    @GetMapping
    public ApiResponse<List<DoctorResponse>> getAllDoctors(
            @RequestParam(required = false) Long specialtyId,
            @RequestParam(required = false) EStatus status) {
        return ApiResponse.<List<DoctorResponse>>builder()
                .result(doctorService.getAllDoctors(specialtyId, status))
                .build();
    }

    /**
     * GET /api/admin/doctors/{doctorId}
     * Lấy chi tiết thông tin một bác sĩ theo doctorId.
     */
    @GetMapping("/{doctorId}")
    public ApiResponse<DoctorResponse> getDoctorById(@PathVariable Long doctorId) {
        return ApiResponse.<DoctorResponse>builder()
                .result(doctorService.getDoctor(doctorId))
                .build();
    }

    /**
     * PUT /api/admin/doctors/{doctorId}
     * Cập nhật thông tin bác sĩ (specialty, position, experienceYears, description, status, doctorCode).
     * userId KHÔNG được cập nhật sau khi tạo.
     */
    @PutMapping("/{doctorId}")
    public ApiResponse<DoctorResponse> updateDoctor(
            @PathVariable Long doctorId,
            @RequestBody @Valid DoctorUpdateRequest request) {
        return ApiResponse.<DoctorResponse>builder()
                .result(doctorService.updateDoctor(doctorId, request))
                .build();
    }

    /**
     * DELETE /api/admin/doctors/{doctorId}
     * Xóa hồ sơ bác sĩ theo nghiệp vụ:
     * Luôn vô hiệu hóa (INACTIVE), giữ hồ sơ và lịch sử.
     */
    @DeleteMapping("/{doctorId}")
    public ApiResponse<String> deleteDoctor(@PathVariable Long doctorId) {
        String message = doctorService.deleteDoctor(doctorId);
        return ApiResponse.<String>builder()
                .result(message)
                .build();
    }
}
