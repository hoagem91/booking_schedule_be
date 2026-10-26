package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.*;
import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService service;

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody AdminCreateUserRequest request) {
        return wrap(service.createAdminUser(request));
    }

    @PostMapping("/search")
    public ApiResponse<Page<UserResponse>> search(@Valid @RequestBody UserSearchRequest request,
                                                  @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.<Page<UserResponse>>builder().result(service.search(request, pageable)).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> get(@PathVariable Long id) {
        return wrap(service.getUser(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return wrap(service.updateUser(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<UserResponse> status(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return wrap(service.updateStatus(id, request.getStatus()));
    }

    @PatchMapping("/{id}/roles")
    public ApiResponse<UserResponse> roles(@PathVariable Long id, @Valid @RequestBody UserRolesRequest request) {
        return wrap(service.updateRoles(id, request.getRoles()));
    }

    @PutMapping("/{id}/password")
    public ApiResponse<UserResponse> resetPassword(@PathVariable Long id, @Valid @RequestBody AdminResetPasswordRequest request) {
        return wrap(service.resetPassword(id, request.getNewPassword()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<UserDeleteResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.<UserDeleteResponse>builder().result(service.deleteUser(id)).build();
    }

    @GetMapping("/{id}/audit-logs")
    public ApiResponse<Page<UserAuditResponse>> auditLogs(@PathVariable Long id,
                                                          @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<Page<UserAuditResponse>>builder().result(service.auditLogs(id, pageable)).build();
    }

    private ApiResponse<UserResponse> wrap(UserResponse response) {
        return ApiResponse.<UserResponse>builder().result(response).build();
    }
}
