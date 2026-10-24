package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.ServiceRequest;
import com.booking_schedule.examine.dto.request.ServiceSearchRequest;
import com.booking_schedule.examine.dto.request.ServiceUpdateRequest;
import com.booking_schedule.examine.dto.request.ServiceStatusRequest;
import com.booking_schedule.examine.dto.response.ServiceDeleteResponse;
import com.booking_schedule.examine.dto.response.ApiResponse;
import com.booking_schedule.examine.dto.response.ServiceResponse;
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
@RequestMapping({"/api/admin/services", "/admin/services"})
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
    ApiResponse<ServiceDeleteResponse> deleteService(@PathVariable Long id){
        return ApiResponse.<ServiceDeleteResponse>builder()
                .result(servicesService.deleteService(id))
                .build();
    }

    @PatchMapping("/{id}/status")
    ApiResponse<ServiceResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody ServiceStatusRequest request) {
        return ApiResponse.<ServiceResponse>builder()
                .result(servicesService.updateStatus(id, request.getStatus())).build();
    }

    @PostMapping("/search")
    ApiResponse<Page<ServiceResponse>> getAllServices(@Valid @RequestBody ServiceSearchRequest request,
            @PageableDefault(size = 20, sort = "id") Pageable pageable){
        return ApiResponse.<Page<ServiceResponse>>builder()
                .result(servicesService.searchServices(request.getKeyword(), request.getSpecialtyId(), request.getStatus(), pageable))
                .build();
    }

    @GetMapping("/{id}")
    ApiResponse<ServiceResponse> getServiceById(@PathVariable Long id){
        return ApiResponse.<ServiceResponse>builder()
                .result(servicesService.getServiceById(id))
                .build();
    }
}
