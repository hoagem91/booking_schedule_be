package com.booking_schedule.examine.mapper;

import com.booking_schedule.examine.dto.request.PatientCreateRequest;
import com.booking_schedule.examine.dto.request.PatientUpdateRequest;
import com.booking_schedule.examine.dto.response.PatientResponse;
import com.booking_schedule.examine.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PatientMapper {

    // toPatient từ request: userId, user sẽ được set thủ công trong Service
    Patient toPatient(PatientCreateRequest request);

    // Phẳng hoá: lấy các field từ patient.user ra ngoài PatientResponse
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.fullName", target = "fullName")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.phone", target = "phone")
    PatientResponse toPatientResponse(Patient patient);

    void updatePatient(@MappingTarget Patient patient, PatientUpdateRequest request);
}
