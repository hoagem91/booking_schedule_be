package com.booking_schedule.examine.controller;
import com.booking_schedule.examine.dto.request.VietqrCallbackRequest;
import com.booking_schedule.examine.dto.response.ApiResponse;
import com.booking_schedule.examine.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequiredArgsConstructor
public class VietqrController {
    private final VietqrService service;
    private final VietqrAuthService auth;
    @PostMapping("/api/patient/appointments/{id}/payments/vietqr")
    public ApiResponse<VietqrService.QrResponse> create(@PathVariable Long id) {
        return ApiResponse.<VietqrService.QrResponse>builder().result(service.create(id)).build();
    }
    @GetMapping("/api/patient/payments/vietqr/{txnRef}")
    public ApiResponse<VietqrService.StatusResponse> status(@PathVariable String txnRef) {
        return ApiResponse.<VietqrService.StatusResponse>builder().result(service.status(txnRef)).build();
    }
    @PostMapping("/api/token_generate")
    public Map<String,Object> token(@RequestHeader(value="Authorization",required=false) String header) { return auth.issue(header); }
    @PostMapping("/bank/api/transaction-sync")
    public Map<String,Object> sync(@RequestHeader(value="Authorization",required=false) String header,
            @Valid @RequestBody VietqrCallbackRequest request) {
        auth.verify(header);
        String reference=service.receive(request);
        return Map.of("error",false,"errorReason","","toastMessage","Transaction processed",
                "object",Map.of("reftransactionid",reference));
    }
}
