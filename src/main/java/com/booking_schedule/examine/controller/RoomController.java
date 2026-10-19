package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.*;
import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService service;

    @PostMapping("/search")
    public ApiResponse<Page<RoomResponse>> search(@Valid @RequestBody RoomSearchRequest request,
            @PageableDefault(sort = "id") Pageable pageable) {
        return ApiResponse.<Page<RoomResponse>>builder()
                .result(service.search(request.getKeyword(), request.getSpecialtyId(), request.getStatus(), pageable)).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomResponse> get(@PathVariable Long id) {
        return ApiResponse.<RoomResponse>builder().result(service.getRoom(id)).build();
    }

    @PostMapping
    public ApiResponse<RoomResponse> create(@Valid @RequestBody RoomRequest request) {
        return ApiResponse.<RoomResponse>builder().result(service.createRoom(request)).build();
    }

    @PutMapping("/{id}")
    public ApiResponse<RoomResponse> update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return ApiResponse.<RoomResponse>builder().result(service.updateRoom(id, request)).build();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<RoomResponse> status(@PathVariable Long id, @Valid @RequestBody RoomStatusRequest request) {
        return ApiResponse.<RoomResponse>builder().result(service.updateStatus(id, request.getStatus())).build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        return ApiResponse.<String>builder().result(service.deleteRoom(id)).build();
    }
}
