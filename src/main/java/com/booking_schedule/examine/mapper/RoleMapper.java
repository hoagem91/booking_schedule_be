package com.booking_schedule.examine.mapper;

import com.booking_schedule.examine.dto.response.RoleResponse;
import com.booking_schedule.examine.entity.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    Role toRole(String roleName);

    RoleResponse toRoleResponse(Role role);
}
