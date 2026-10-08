package com.booking_schedule.examine.service;

import com.booking_schedule.examine.constant.PredefinedRole;
import com.booking_schedule.examine.dto.request.CreateUserRequest;
import com.booking_schedule.examine.dto.request.UpdateUserRequest;
import com.booking_schedule.examine.dto.response.UserResponse;
import com.booking_schedule.examine.entity.Role;
import com.booking_schedule.examine.entity.User;
import com.booking_schedule.examine.enums.ERole;
import com.booking_schedule.examine.exception.AppException;
import com.booking_schedule.examine.exception.ErrorCode;
import com.booking_schedule.examine.mapper.UserMapper;
import com.booking_schedule.examine.repository.RoleRepository;
import com.booking_schedule.examine.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    public UserResponse createUser(CreateUserRequest request){
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        HashSet<Role> roles = new HashSet<>();

        roleRepository.findByName(ERole.PATIENT).ifPresent(roles::add);

        user.setRoles(roles);
        try{
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException ex){
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long userId,UpdateUserRequest request){
        User user = userRepository.findById(userId).orElseThrow(()-> new AppException(ErrorCode.USER_NOT_EXISTED));

        userMapper.updateUser(user,request);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            List<ERole> eRoles = request.getRoles().stream()
                    .map(roleName -> {
                        try {
                            return ERole.valueOf(roleName);
                        } catch (IllegalArgumentException e) {
                            throw new AppException(ErrorCode.INVALID_ROLE);
                        }
                    })
                    .toList();

            List<Role> roles = roleRepository.findByNameIn(eRoles);
            user.setRoles(new HashSet<>(roles));
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    public UserResponse getMyInfo() {
        var context = org.springframework.security.core.context.SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    public List<UserResponse> getUsers() {
        try{
            return userRepository.findAll().stream()
                    .map(userMapper::toUserResponse)
                    .toList();
        }catch (Exception ex){
            log.error("Error while fetching users: {}", ex.getMessage());
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    public UserResponse getUser(Long id) {
        return userMapper.toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }

    public void deleteUser(Long userId){
        userRepository.deleteById(userId);
    }
}
