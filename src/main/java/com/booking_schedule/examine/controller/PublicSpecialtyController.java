package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.response.ApiResponse;
import com.booking_schedule.examine.dto.response.SpecialtyResponse;
import com.booking_schedule.examine.enums.EStatus;
import com.booking_schedule.examine.service.SpecialtyService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/specialties")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PublicSpecialtyController {
    SpecialtyService specialtyService;

    @GetMapping
    public ApiResponse<List<SpecialtyResponse>> getActiveSpecialties() {
        return ApiResponse.<List<SpecialtyResponse>>builder()
                .result(specialtyService.getAllSpecialties(EStatus.ACTIVE))
                .build();
    }
}
