package com.booking_schedule.examine.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
@RestControllerAdvice(assignableTypes=VietqrController.class) @Order(0)
public class VietqrExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("error",true,
                "errorReason",exception.getReason()==null ? "REQUEST_REJECTED" : exception.getReason(),"toastMessage","Request rejected"));
    }
    @ExceptionHandler(com.booking_schedule.examine.exception.AppException.class)
    public ResponseEntity<?> business(com.booking_schedule.examine.exception.AppException exception) {
        var error=exception.getErrorCode();
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("error",true,
                "errorReason",error.name(),"toastMessage",error.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> failure(Exception exception) {
        boolean bad=exception instanceof org.springframework.web.bind.MethodArgumentNotValidException
                || exception instanceof org.springframework.http.converter.HttpMessageNotReadableException;
        return ResponseEntity.status(bad ? HttpStatus.BAD_REQUEST : HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error",true,"errorReason",bad ? "INVALID_PAYLOAD" : "TEMPORARILY_UNAVAILABLE",
                        "toastMessage","Unable to process payment request"));
    }
}
