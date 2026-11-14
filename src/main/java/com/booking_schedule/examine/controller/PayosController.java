package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.response.ApiResponse;
import com.booking_schedule.examine.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequiredArgsConstructor
public class PayosController {
    private final PayosService service;
    @PostMapping("/api/patient/appointments/{id}/payments/payos")
    public ApiResponse<PayosPaymentStore.Checkout> create(@PathVariable Long id) {
        return ApiResponse.<PayosPaymentStore.Checkout>builder().result(service.create(id)).build();
    }
    @GetMapping("/api/patient/payments/payos/{txnRef}")
    public ApiResponse<PayosPaymentStore.Status> status(@PathVariable String txnRef) {
        return ApiResponse.<PayosPaymentStore.Status>builder().result(service.status(txnRef)).build();
    }
    @PostMapping("/api/patient/payments/payos/{txnRef}/refresh")
    public ApiResponse<PayosPaymentStore.Status> refresh(@PathVariable String txnRef) {
        return ApiResponse.<PayosPaymentStore.Status>builder().result(service.refresh(txnRef)).build();
    }
    @PostMapping(value="/api/payments/payos/webhook", consumes=MediaType.APPLICATION_JSON_VALUE)
    public Map<String,Object> webhook(@RequestBody String raw) {
        service.webhook(raw);
        return Map.of("code","00","desc","success");
    }
}
