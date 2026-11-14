package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.service.PaymentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
public class PatientPaymentController {
    private final PaymentQueryService service;
    @GetMapping("/api/patient/payments")
    public ApiResponse<Page<PaymentResponse>> mine(@PageableDefault(size=20) Pageable page) {
        return ApiResponse.<Page<PaymentResponse>>builder().result(service.mine(page)).build();
    }
    @GetMapping("/api/patient/payments/{id}")
    public ApiResponse<PaymentResponse> detail(@PathVariable Long id) {
        return ApiResponse.<PaymentResponse>builder().result(service.mineDetail(id)).build();
    }
    @GetMapping("/api/patient/appointments/{id}/payment")
    public ApiResponse<PaymentResponse> appointment(@PathVariable Long id) {
        return ApiResponse.<PaymentResponse>builder().result(service.mineByAppointment(id)).build();
    }
    @GetMapping("/api/patient/payments/{id}/transactions")
    public ApiResponse<Page<PaymentTransactionResponse>> transactions(@PathVariable Long id,@PageableDefault(size=20) Pageable page) {
        return ApiResponse.<Page<PaymentTransactionResponse>>builder().result(service.mineTransactions(id,page)).build();
    }
}
