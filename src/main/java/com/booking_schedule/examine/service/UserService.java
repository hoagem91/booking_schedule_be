package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.request.*;
import com.booking_schedule.examine.dto.response.UserResponse;
import com.booking_schedule.examine.entity.*;
import com.booking_schedule.examine.enums.ERole;
import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.mapper.UserMapper;
import com.booking_schedule.examine.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /** Admin provisions an account; roles are granted explicitly or with profile creation. */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        requireAdmin();
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(new HashSet<>());
        return save(user);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User actor = requireAdmin();
        User user = locked(userId);
        protectSuperAdmin(actor, user);
        Set<ERole> before = roleNames(user);
        if (request.getRoles() != null) {
            if (request.getRoles().isEmpty()) throw new AppException(ErrorCode.INVALID_ROLE);
            Set<ERole> desired = new HashSet<>();
            try {
                request.getRoles().forEach(name -> desired.add(ERole.valueOf(name)));
            } catch (IllegalArgumentException | NullPointerException ex) {
                throw new AppException(ErrorCode.INVALID_ROLE);
            }
            if (desired.contains(ERole.SUPER_ADMIN) && !roleNames(actor).contains(ERole.SUPER_ADMIN))
                throw new AppException(ErrorCode.UNAUTHORIZED);
            if (patientRepository.existsByUserId(userId) != desired.contains(ERole.PATIENT)
                    || doctorRepository.existsByUserId(userId) != desired.contains(ERole.DOCTOR))
                throw new AppException(ErrorCode.ROLE_PROFILE_CONFLICT);
            List<Role> roles = roleRepository.findByNameIn(new ArrayList<>(desired));
            if (roles.size() != desired.size()) throw new AppException(ErrorCode.INVALID_ROLE);
            user.setRoles(new HashSet<>(roles));
        }
        String oldStatus = user.getStatus();
        userMapper.updateUser(user, request);
        if (request.getPassword() != null) user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (request.getPassword() != null || !Objects.equals(oldStatus, user.getStatus()) || !before.equals(roleNames(user)))
            user.revokeTokens();
        return save(user);
    }

    @Transactional
    public UserResponse updateMyInfo(UpdateMyUserRequest request) {
        User user = locked(currentUser().getId());
        userMapper.updateMyUser(user, request);
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.revokeTokens();
        }
        return save(user);
    }

    public UserResponse getMyInfo() { return userMapper.toUserResponse(currentUser()); }
    public List<UserResponse> getUsers() {
        requireAdmin();
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }
    public UserResponse getUser(Long id) {
        requireAdmin();
        return userMapper.toUserResponse(userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }

    /** Preserve profiles, appointments, reviews and notifications. */
    @Transactional
    public void deleteUser(Long userId) {
        User actor = requireAdmin();
        User user = locked(userId);
        protectSuperAdmin(actor, user);
        user.setStatus("INACTIVE");
        user.revokeTokens();
        userRepository.save(user);
    }

    private User locked(Long id) {
        return userRepository.findLockedById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AppException(ErrorCode.UNAUTHENTICATED);
        User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        if (!"ACTIVE".equals(user.getStatus())) throw new AppException(ErrorCode.USER_INACTIVE);
        return user;
    }
    private User requireAdmin() {
        User user = currentUser();
        Set<ERole> roles = roleNames(user);
        if (!roles.contains(ERole.ADMIN) && !roles.contains(ERole.SUPER_ADMIN)) throw new AppException(ErrorCode.UNAUTHORIZED);
        return user;
    }
    private void protectSuperAdmin(User actor, User target) {
        if (roleNames(target).contains(ERole.SUPER_ADMIN) && !roleNames(actor).contains(ERole.SUPER_ADMIN))
            throw new AppException(ErrorCode.UNAUTHORIZED);
    }
    private Set<ERole> roleNames(User user) {
        Set<ERole> names = new HashSet<>();
        user.getRoles().forEach(role -> names.add(role.getName()));
        return names;
    }
    private UserResponse save(User user) {
        try { return userMapper.toUserResponse(userRepository.saveAndFlush(user)); }
        catch (DataIntegrityViolationException ex) { throw new AppException(ErrorCode.USER_EXISTED); }
    }
}
