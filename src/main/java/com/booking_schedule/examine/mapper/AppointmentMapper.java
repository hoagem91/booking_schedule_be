package com.booking_schedule.examine.mapper;

import com.booking_schedule.examine.dto.response.AppointmentResponse;
import com.booking_schedule.examine.entity.Appointment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    @org.mapstruct.Mapping(source = "patient.id", target = "patientId")
    @org.mapstruct.Mapping(source = "patient.patientCode", target = "patientCode")
    @org.mapstruct.Mapping(source = "patient.user.fullName", target = "patientName")
    @org.mapstruct.Mapping(source = "patient.user.phone", target = "patientPhone")
    @org.mapstruct.Mapping(source = "doctor.id", target = "doctorId")
    @org.mapstruct.Mapping(source = "doctor.doctorCode", target = "doctorCode")
    @org.mapstruct.Mapping(source = "doctor.user.fullName", target = "doctorName")
    @org.mapstruct.Mapping(source = "service.id", target = "serviceId")
    @org.mapstruct.Mapping(source = "service.name", target = "serviceName")
    @org.mapstruct.Mapping(source = "timeSlot.id", target = "timeSlotId")
    @org.mapstruct.Mapping(source = "timeSlot.startTime", target = "startTime")
    @org.mapstruct.Mapping(source = "timeSlot.endTime", target = "endTime")
    @org.mapstruct.Mapping(source = "promotion.id", target = "promotionId")
    @org.mapstruct.Mapping(source = "promotion.code", target = "promotionCode")
    @org.mapstruct.Mapping(source = "promotion.name", target = "promotionName")
    @org.mapstruct.Mapping(source = "promotion.discountType", target = "discountType")
    @org.mapstruct.Mapping(source = "promotion.discountValue", target = "discountValue")
    @org.mapstruct.Mapping(source = "promotion.maxDiscount", target = "maxDiscount")
    @org.mapstruct.Mapping(target = "reviewEligible", ignore = true)
    AppointmentResponse toAppointmentResponse(Appointment appointment);
}
