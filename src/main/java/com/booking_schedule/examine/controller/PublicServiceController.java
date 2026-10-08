package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.response.ApiResponse;
import com.booking_schedule.examine.dto.response.ServiceResponse;
import com.booking_schedule.examine.service.ServicesService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/services")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PublicServiceController {
    ServicesService servicesService;

    @GetMapping
    ApiResponse<List<ServiceResponse>> getAllServices(@RequestParam(required = false) Long specialtyId){
        return ApiResponse.<List<ServiceResponse>>builder()
                .result(servicesService.getPublicServices(specialtyId))
                .build();
    }
}
