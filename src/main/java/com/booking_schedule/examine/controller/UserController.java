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

    @PostMapping("/registration")
    ApiResponse<UserResponse> createUser(@RequestBody @Valid CreateUserRequest request){
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
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
                .result("User has been deleted")
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
