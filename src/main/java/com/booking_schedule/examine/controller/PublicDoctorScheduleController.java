package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.service.DoctorScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/public/doctor-schedules")
@RequiredArgsConstructor
public class PublicDoctorScheduleController {
    private final DoctorScheduleService service;

    @GetMapping
    public ApiResponse<List<DoctorScheduleResponse>> search(@RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long specialtyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.<List<DoctorScheduleResponse>>builder()
                .result(service.publicSchedules(doctorId, specialtyId, fromDate, toDate)).build();
    }
}
