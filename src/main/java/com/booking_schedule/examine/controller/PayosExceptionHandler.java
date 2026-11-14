package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.response.ApiResponse;
import com.booking_schedule.examine.exception.AppException;
import com.booking_schedule.examine.service.PayosService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice(assignableTypes={PayosController.class,AdminPaymentController.class}) @Order(0) @Slf4j
public class PayosExceptionHandler {
    @ExceptionHandler(PayosService.CreationUncertain.class)
    public ResponseEntity<?> uncertain(PayosService.CreationUncertain exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.builder().code(9101)
                .message("Chưa xác định được kết quả tạo link. Hãy kiểm tra lại giao dịch trước khi thanh toán.")
                .result(Map.of("txnRef",exception.txnRef)).build());
    }
    @ExceptionHandler(AppException.class)
    public ResponseEntity<?> business(AppException exception) {
        var error=exception.getErrorCode();
        return ResponseEntity.status(error.getStatusCode()).body(ApiResponse.builder()
                .code(error.getCode()).message(error.getMessage()).build());
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(ApiResponse.builder().code(9102)
                .message(exception.getReason()).build());
    }
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> invalidRequest(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.builder().code(9202).message("Invalid payment request or filters").build());
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> failure(Exception exception) {
        Throwable cause=exception;
        for (int depth=0;depth<8 && cause.getCause()!=null && cause.getCause()!=cause;depth++) cause=cause.getCause();
        String origin=java.util.Arrays.stream(cause.getStackTrace())
                .filter(frame -> frame.getClassName().startsWith("com.booking_schedule."))
                .findFirst().map(StackTraceElement::toString).orElse("provider/persistence");
        // No exception message, body or credentials are logged/exposed.
        log.warn("payOS request failed ({}, root={}, at={})",exception.getClass().getSimpleName(),cause.getClass().getSimpleName(),origin);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.builder().code(9103)
                .message("Chưa xử lý được thanh toán. Vui lòng kiểm tra trạng thái giao dịch trước khi thử lại.").build());
    }
}
