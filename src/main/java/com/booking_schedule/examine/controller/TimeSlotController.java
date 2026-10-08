package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class TimeSlotController {
    private final TimeSlotService service;

    @GetMapping("/time-slots")
    public ApiResponse<List<TimeSlotResponse>> get(@RequestParam Long scheduleId) {
        return ApiResponse.<List<TimeSlotResponse>>builder().result(service.getAllTimeSlot(scheduleId)).build();
    }

    @GetMapping({"/doctor-schedules/{scheduleId}/time-slots", "/time-slots/{scheduleId}/time-slots"})
    public ApiResponse<List<TimeSlotResponse>> getBySchedule(@PathVariable Long scheduleId) {
        return get(scheduleId);
    }
}
