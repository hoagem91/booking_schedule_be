package com.booking_schedule.examine.mapper;

import com.booking_schedule.examine.dto.request.DoctorCreateRequest;
import com.booking_schedule.examine.dto.request.DoctorUpdateRequest;
import com.booking_schedule.examine.dto.response.DoctorResponse;
import com.booking_schedule.examine.entity.Doctor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DoctorMapper {

    // toDoctor: user và specialty được set thủ công trong Service
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "specialty", ignore = true)
    @Mapping(target = "status", ignore = true)
    Doctor toDoctor(DoctorCreateRequest request);

    // Phẳng hoá user.* và specialty.* vào DoctorResponse
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.fullName", target = "fullName")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.phone", target = "phone")
    @Mapping(source = "specialty.id", target = "specialtyId")
    @Mapping(source = "specialty.name", target = "specialtyName")
    DoctorResponse toDoctorResponse(Doctor doctor);

    // updateDoctor: KHÔNG cho cập nhật user và doctorCode
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "doctorCode", ignore = true)
    @Mapping(target = "specialty", ignore = true)
    void updateDoctorFromRequest(@MappingTarget Doctor doctor, DoctorUpdateRequest request);
}
