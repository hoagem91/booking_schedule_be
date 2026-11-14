package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.*;
import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/payments") @RequiredArgsConstructor
public class AdminPaymentController {
    private final PaymentQueryService service;
    @PostMapping("/search")
    public ApiResponse<Page<PaymentResponse>> search(@Valid @RequestBody PaymentSearchRequest request,
            @PageableDefault(size=20) Pageable page) {
        return ApiResponse.<Page<PaymentResponse>>builder().result(service.search(request,page)).build();
    }
    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> detail(@PathVariable Long id) {
        return ApiResponse.<PaymentResponse>builder().result(service.adminDetail(id)).build();
    }
    @PostMapping("/transactions/search")
    public ApiResponse<Page<PaymentTransactionResponse>> transactions(@Valid @RequestBody PaymentTransactionSearchRequest request,
            @PageableDefault(size=20) Pageable page) {
        return ApiResponse.<Page<PaymentTransactionResponse>>builder().result(service.adminTransactions(request,page)).build();
    }
    @GetMapping("/transactions/{txnRef}")
    public ApiResponse<PaymentTransactionResponse> transaction(@PathVariable String txnRef) {
        return ApiResponse.<PaymentTransactionResponse>builder().result(service.adminTransaction(txnRef)).build();
    }
    @PostMapping("/transactions/{txnRef}/refresh")
    public ApiResponse<PayosPaymentStore.Status> refresh(@PathVariable String txnRef) {
        return ApiResponse.<PayosPaymentStore.Status>builder().result(service.adminRefresh(txnRef)).build();
    }
}
