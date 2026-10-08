package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.response.RoleResponse;
import com.booking_schedule.examine.mapper.RoleMapper;
import com.booking_schedule.examine.repository.RoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RoleService {
    RoleRepository roleRepository;
    RoleMapper roleMapper;

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream().map(roleMapper::toRoleResponse).toList();
    }
}
