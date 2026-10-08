package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.TimeSlotStatusRequest;
import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.service.TimeSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/time-slots")
@RequiredArgsConstructor
public class AdminTimeSlotController {
    private final TimeSlotService service;

    @PatchMapping("/{id}/status")
    public ApiResponse<TimeSlotResponse> changeStatus(@PathVariable Long id, @Valid @RequestBody TimeSlotStatusRequest request) {
        return ApiResponse.<TimeSlotResponse>builder().result(service.changeStatus(id, request.getStatus())).build();
    }
}
