package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.PublicDoctorScheduleSearchRequest;
import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.service.DoctorScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/public/doctor-schedules")
@RequiredArgsConstructor
public class PublicDoctorScheduleController {
    private final DoctorScheduleService service;

    @PostMapping("/search")
    public ApiResponse<List<DoctorScheduleResponse>> search(@Valid @RequestBody PublicDoctorScheduleSearchRequest request) {
        return ApiResponse.<List<DoctorScheduleResponse>>builder()
                .result(service.publicSchedules(request.getDoctorId(), request.getSpecialtyId(),
                        request.getFromDate(), request.getToDate())).build();
    }
}
