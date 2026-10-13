package com.booking_schedule.examine.mapper;

import com.booking_schedule.examine.dto.request.CreateUserRequest;
import com.booking_schedule.examine.dto.request.UpdateUserRequest;
import com.booking_schedule.examine.dto.response.UserResponse;
import com.booking_schedule.examine.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    User toUser(CreateUserRequest request);

    UserResponse toUserResponse(User user);

    @Mapping(target = "roles",ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateUser(@MappingTarget User user, UpdateUserRequest request);

    @Mapping(target = "password", ignore = true)
    void updateMyUser(@MappingTarget User user, com.booking_schedule.examine.dto.request.UpdateMyUserRequest request);
}
