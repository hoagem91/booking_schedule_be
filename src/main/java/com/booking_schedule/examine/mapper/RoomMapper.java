package com.booking_schedule.examine.mapper;

import com.booking_schedule.examine.entity.Room;
import com.booking_schedule.examine.dto.response.RoomResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    @Mapping(source = "specialty.id", target = "specialtyId")
    @Mapping(source = "specialty.name", target = "specialtyName")
    RoomResponse toResponse(Room room);
}
