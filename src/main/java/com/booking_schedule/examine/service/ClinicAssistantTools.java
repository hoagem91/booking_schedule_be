package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.response.ServiceResponse;
import com.booking_schedule.examine.dto.response.SpecialtyResponse;
import com.booking_schedule.examine.enums.EStatus;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Read-only clinic information exposed to the assistant. */
@Component
@RequiredArgsConstructor
public class ClinicAssistantTools {
    private final SpecialtyService specialtyService;
    private final ServicesService servicesService;

    @Tool(description = "Liệt kê các chuyên khoa đang hoạt động của phòng khám.")
    public List<SpecialtyInfo> listActiveSpecialties() {
        return specialtyService.getAllSpecialties(EStatus.ACTIVE).stream()
                .map(specialty -> new SpecialtyInfo(
                        specialty.getId(), specialty.getName(), specialty.getDescription()))
                .toList();
    }

    @Tool(description = "Tra cứu các dịch vụ khám công khai, có thể lọc theo mã chuyên khoa.")
    public List<ServiceInfo> listPublicServices(
            @ToolParam(description = "Mã chuyên khoa; bỏ trống để lấy tất cả dịch vụ công khai", required = false)
                    Long specialtyId) {
        return servicesService.getPublicServices(specialtyId).stream()
                .map(ClinicAssistantTools::toServiceInfo)
                .toList();
    }

    private static ServiceInfo toServiceInfo(ServiceResponse service) {
        return new ServiceInfo(
                service.getId(),
                service.getName(),
                service.getSpecialtyName(),
                service.getPrice(),
                service.getDurationMinutes(),
                service.getDescription());
    }

    public record SpecialtyInfo(Long id, String name, String description) {}

    public record ServiceInfo(
            Long id,
            String name,
            String specialtyName,
            BigDecimal price,
            Integer durationMinutes,
            String description) {}
}
