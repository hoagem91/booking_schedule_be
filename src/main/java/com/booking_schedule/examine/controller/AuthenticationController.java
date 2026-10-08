package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.AuthenticationRequest;
import com.booking_schedule.examine.dto.request.IntrospectRequest;
import com.booking_schedule.examine.dto.request.LogoutRequest;
import com.booking_schedule.examine.dto.request.RefreshTokenRequest;
import com.booking_schedule.examine.dto.response.ApiResponse;
import com.booking_schedule.examine.dto.response.AuthenticationResponse;
import com.booking_schedule.examine.dto.response.IntrospectResponse;
import com.booking_schedule.examine.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    AuthenticationService authenticationService;

    /**
     * POST /auth/token
     * Đăng nhập và nhận về JWT Access Token
     */
    @PostMapping("/token")
    public ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        AuthenticationResponse result = authenticationService.authenticate(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    /**
     * POST /auth/introspect
     * Kiểm tra tính hợp lệ của token (dùng cho gateway hoặc debug)
     */
    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) {
        IntrospectResponse result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    /**
     * POST /auth/logout
     * Đăng xuất: đưa token vào blacklist trong DB
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder().build();
    }

    /**
     * POST /auth/refresh
     * Làm mới token: thu hồi token cũ, cấp token mới
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> refreshToken(@RequestBody RefreshTokenRequest request)
            throws ParseException, JOSEException {
        AuthenticationResponse result = authenticationService.refreshToken(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }
}
