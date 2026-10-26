package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.*;
import com.booking_schedule.examine.dto.request.RegisterRequest;
import com.booking_schedule.examine.dto.request.UpdateMyUserRequest;
import com.booking_schedule.examine.dto.request.UpdateUserRequest;
import com.booking_schedule.examine.dto.response.ApiResponse;
import com.booking_schedule.examine.dto.response.UserResponse;
import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.service.AuthenticationService;
import com.booking_schedule.examine.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;
    AuthenticationService authenticationService;

    @PostMapping("/registration")
    ApiResponse<com.booking_schedule.examine.dto.response.AuthenticationResponse> registerPatient(
            @RequestBody @Valid RegisterRequest request){
        return ApiResponse.<com.booking_schedule.examine.dto.response.AuthenticationResponse>builder()
                .result(authenticationService.registerPatient(request))
                .build();
    }

    @PostMapping
    ApiResponse<UserResponse> createUser(@RequestBody @Valid AdminCreateUserRequest request) {
        return ApiResponse.<UserResponse>builder().result(userService.createUser(request)).build();
    }

    @GetMapping("/me")
    ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder().result(userService.getMyInfo()).build();
    }

    @PutMapping("/me")
    ApiResponse<UserResponse> updateMyInfo(@RequestBody @Valid UpdateMyUserRequest request) {
        return ApiResponse.<UserResponse>builder().result(userService.updateMyInfo(request)).build();
    }

    @PutMapping("/me/password")
    ApiResponse<UserResponse> changeMyPassword(@Valid @RequestBody ChangeMyPasswordRequest request) {
        return ApiResponse.<UserResponse>builder().result(userService.changeMyPassword(request)).build();
    }

    @PostMapping("/search")
    ApiResponse<Page<UserResponse>> search(@Valid @RequestBody UserSearchRequest request,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.<Page<UserResponse>>builder().result(userService.search(request, pageable)).build();
    }

    @PatchMapping("/{userId}/status")
    ApiResponse<UserResponse> status(@PathVariable Long userId, @Valid @RequestBody UserStatusRequest request) {
        return ApiResponse.<UserResponse>builder().result(userService.updateStatus(userId, request.getStatus())).build();
    }

    @PatchMapping("/{userId}/roles")
    ApiResponse<UserResponse> roles(@PathVariable Long userId, @Valid @RequestBody UserRolesRequest request) {
        return ApiResponse.<UserResponse>builder().result(userService.updateRoles(userId, request.getRoles())).build();
    }

    @PutMapping("/{userId}/password")
    ApiResponse<UserResponse> resetPassword(@PathVariable Long userId, @Valid @RequestBody AdminResetPasswordRequest request) {
        return ApiResponse.<UserResponse>builder().result(userService.resetPassword(userId, request.getNewPassword())).build();
    }

    @GetMapping("/{userId}/audit-logs")
    ApiResponse<Page<UserAuditResponse>> auditLogs(@PathVariable Long userId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<Page<UserAuditResponse>>builder().result(userService.auditLogs(userId, pageable)).build();
    }

    @PutMapping("/{userId}")
    ApiResponse<UserResponse> updateUser(@PathVariable Long userId, @RequestBody @Valid UpdateUserRequest request){
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(userId,request))
                .build();
    }

    @DeleteMapping("/{userId}")
    ApiResponse<UserDeleteResponse> deleteUser(@PathVariable Long userId){
        return ApiResponse.<UserDeleteResponse>builder()
                .result(userService.deleteUser(userId))
                .build();
    }

    @GetMapping("/{userId}")
    ApiResponse<UserResponse> getUserById(@PathVariable Long userId){
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUser(userId))
                .build();
    }

    @GetMapping("/all")
    ApiResponse<List<UserResponse>> getAllUsers(){
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUsers())
                .build();
    }
}
