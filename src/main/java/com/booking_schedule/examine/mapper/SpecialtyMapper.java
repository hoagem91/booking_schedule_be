package com.booking_schedule.examine.mapper;

import com.booking_schedule.examine.dto.request.SpecialtyRequest;
import com.booking_schedule.examine.dto.request.SpecialtyUpdateRequest;
import com.booking_schedule.examine.dto.response.SpecialtyResponse;
import com.booking_schedule.examine.entity.Specialty;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SpecialtyMapper {

    // toSpecialty: status mặc định ACTIVE — được set bởi @Builder.Default trong entity
    // doctors, services là collection — không map từ request
    @Mapping(target = "doctors", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "status", ignore = true)
    Specialty toSpecialty(SpecialtyRequest request);

    // toSpecialtyResponse: map trực tiếp các field scalar
    // doctors, services là List — KHÔNG đưa vào response (tránh N+1, lazy loading)
    SpecialtyResponse toSpecialtyResponse(Specialty specialty);

    // updateSpecialty: ignore collections và id (không cho update)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "doctors", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateSpecialty(@MappingTarget Specialty specialty, SpecialtyUpdateRequest request);
}
