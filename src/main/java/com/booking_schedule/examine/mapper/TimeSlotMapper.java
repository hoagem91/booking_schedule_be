package com.booking_schedule.examine.mapper;

import com.booking_schedule.examine.dto.response.TimeSlotResponse;
import com.booking_schedule.examine.entity.TimeSlot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TimeSlotMapper {
    @Mapping(target = "scheduleId", source = "doctorSchedule.id")
    TimeSlotResponse toTimeSlotResponse(TimeSlot timeSlot);
}
