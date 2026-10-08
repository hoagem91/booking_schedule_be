package com.booking_schedule.examine.mapper;

import com.booking_schedule.examine.dto.request.DoctorScheduleCreateRequest;
import com.booking_schedule.examine.dto.response.DoctorScheduleResponse;
import com.booking_schedule.examine.entity.DoctorSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.booking_schedule.examine.entity.TimeSlot;
import com.booking_schedule.examine.dto.response.TimeSlotResponse;

@Mapper(componentModel = "spring")
public interface DoctorScheduleMapper {
    @Mapping(target = "doctor", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "timeSlots", ignore = true)
    @Mapping(target = "status", constant = "OPEN")
    @Mapping(target = "maxPatients", source = "maxPatients", defaultValue = "20")
    DoctorSchedule toDoctorSchedule(DoctorScheduleCreateRequest request);
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorCode", source = "doctor.doctorCode")
    @Mapping(target = "doctorName", source = "doctor.user.fullName")
    @Mapping(target = "specialtyName", source = "doctor.specialty.name")
    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomCode", source = "room.code")
    @Mapping(target = "roomName", source = "room.name")
    DoctorScheduleResponse toDoctorScheduleResponse(DoctorSchedule doctorSchedule);
    @Mapping(target = "scheduleId", source = "doctorSchedule.id")
    TimeSlotResponse toTimeSlotResponse(TimeSlot slot);
}
