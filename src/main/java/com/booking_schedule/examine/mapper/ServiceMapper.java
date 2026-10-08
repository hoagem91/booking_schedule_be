package com.booking_schedule.examine.mapper;

import com.booking_schedule.examine.dto.request.ServiceRequest;
import com.booking_schedule.examine.dto.request.ServiceUpdateRequest;
import com.booking_schedule.examine.dto.response.ServiceResponse;
import com.booking_schedule.examine.entity.Service;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ServiceMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "specialty", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "durationMinutes", source = "durationMinutes", defaultValue = "30")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Service toService(ServiceRequest request);
    @Mapping(target = "specialtyId", source = "specialty.id")
    ServiceResponse toServiceResponse(Service service);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "specialty", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateService(@MappingTarget Service service, ServiceUpdateRequest request);
}
