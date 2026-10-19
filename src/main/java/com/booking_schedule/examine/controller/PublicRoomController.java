package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicRoomController {
    private final RoomService service;

    @GetMapping("/rooms")
    public ApiResponse<List<RoomResponse>> rooms() {
        return ApiResponse.<List<RoomResponse>>builder().result(service.publicRooms(null)).build();
    }

    @GetMapping("/specialties/{specialtyId}/rooms")
    public ApiResponse<List<RoomResponse>> roomsBySpecialty(@PathVariable Long specialtyId) {
        return ApiResponse.<List<RoomResponse>>builder().result(service.publicRooms(specialtyId)).build();
    }
}
