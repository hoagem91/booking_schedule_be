package com.booking_schedule.examine.controller;

import com.booking_schedule.examine.dto.request.CreateUserRequest;
import com.booking_schedule.examine.dto.request.UpdateUserRequest;
import com.booking_schedule.examine.dto.response.ApiResponse;
import com.booking_schedule.examine.dto.response.UserResponse;
import com.booking_schedule.examine.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;
    com.booking_schedule.examine.service.AuthenticationService authenticationService;

    @PostMapping("/registration")
    ApiResponse<com.booking_schedule.examine.dto.response.AuthenticationResponse> registerPatient(
            @RequestBody @Valid com.booking_schedule.examine.dto.request.RegisterRequest request){
        return ApiResponse.<com.booking_schedule.examine.dto.response.AuthenticationResponse>builder()
                .result(authenticationService.registerPatient(request))
                .build();
    }

    @PostMapping
    ApiResponse<UserResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
        return ApiResponse.<UserResponse>builder().result(userService.createUser(request)).build();
    }

    @GetMapping("/me")
    ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder().result(userService.getMyInfo()).build();
    }

    @PutMapping("/me")
    ApiResponse<UserResponse> updateMyInfo(@RequestBody @Valid com.booking_schedule.examine.dto.request.UpdateMyUserRequest request) {
        return ApiResponse.<UserResponse>builder().result(userService.updateMyInfo(request)).build();
    }

    @PutMapping("/{userId}")
    ApiResponse<UserResponse> updateUser(@PathVariable Long userId, @RequestBody @Valid UpdateUserRequest request){
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(userId,request))
                .build();
    }

    @DeleteMapping("/{userId}")
    ApiResponse<String> deleteUser(@PathVariable Long userId){
        userService.deleteUser(userId);
        return ApiResponse.<String>builder()
                .result("User account has been deactivated; history is retained")
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
