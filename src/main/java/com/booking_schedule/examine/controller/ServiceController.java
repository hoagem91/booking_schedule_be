package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.ServiceRequest;
import com.booking_schedule.examine.dto.request.ServiceUpdateRequest;
import com.booking_schedule.examine.dto.response.ApiResponse;
import com.booking_schedule.examine.dto.response.ServiceResponse;
import com.booking_schedule.examine.enums.EStatus;
import com.booking_schedule.examine.service.ServicesService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;


@RestController
@RequestMapping("/admin/services")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ServiceController {
    ServicesService servicesService;

    @PostMapping
    ApiResponse<ServiceResponse> createService(@Valid @RequestBody ServiceRequest request){
        return ApiResponse.<ServiceResponse>builder()
                .result(servicesService.createService(request))
                .build();
    }

    @PutMapping("/{id}")
    ApiResponse<ServiceResponse> updateService(@PathVariable Long id, @Valid @RequestBody ServiceUpdateRequest request){
        return ApiResponse.<ServiceResponse>builder()
                .result(servicesService.updateService(id,request))
                .build();
    }

    @DeleteMapping("/{id}")
    ApiResponse<String> deleteService(@PathVariable Long id){
        return ApiResponse.<String>builder()
                .result(servicesService.deleteService(id))
                .build();
    }

    @GetMapping
    ApiResponse<Page<ServiceResponse>> getAllServices(@RequestParam(required = false) EStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long specialtyId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable){
        return ApiResponse.<Page<ServiceResponse>>builder()
                .result(servicesService.searchServices(keyword, specialtyId, status, pageable))
                .build();
    }

    @GetMapping("/{id}")
    ApiResponse<ServiceResponse> getServiceById(@PathVariable Long id){
        return ApiResponse.<ServiceResponse>builder()
                .result(servicesService.getServiceById(id))
                .build();
    }
}
