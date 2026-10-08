package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.request.ServiceRequest;
import com.booking_schedule.examine.dto.request.ServiceUpdateRequest;
import com.booking_schedule.examine.dto.response.ServiceResponse;
import com.booking_schedule.examine.entity.Specialty;
import com.booking_schedule.examine.enums.EStatus;
import com.booking_schedule.examine.exception.AppException;
import com.booking_schedule.examine.exception.ErrorCode;
import com.booking_schedule.examine.mapper.ServiceMapper;
import com.booking_schedule.examine.repository.AppointmentRepository;
import com.booking_schedule.examine.repository.ServiceRepository;
import com.booking_schedule.examine.repository.SpecialtyRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;

import java.text.Normalizer;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional(readOnly = true)
public class ServicesService {
    ServiceMapper serviceMapper;
    ServiceRepository serviceRepository;
    AppointmentRepository appointmentRepository;
    SpecialtyRepository specialtyRepository;

    @Transactional
    public ServiceResponse createService(ServiceRequest request){
        String code = request.getCode();
        if(code == null || code.trim().isEmpty()){
            code = generateCodeFromName(request.getName().trim());
        } else {
            code = code.trim();
            if(serviceRepository.existsByCode(code)){
                throw new AppException(ErrorCode.SERVICE_CODE_EXISTED);
            }
        }
        Specialty specialty = request.getSpecialtyId() == null ? null : getActiveSpecialty(request.getSpecialtyId());

        com.booking_schedule.examine.entity.Service service = com.booking_schedule.examine.entity.Service.builder()
                .code(code)
                .name(request.getName().trim())
                .specialty(specialty)
                .price(request.getPrice())
                .description(request.getDescription())
                .durationMinutes(request.getDurationMinutes() == null ? 30 : request.getDurationMinutes())
                .status(EStatus.ACTIVE)
                .build();

        return serviceMapper.toServiceResponse(saveService(service));
    }

    @Transactional
    public ServiceResponse updateService(Long id, ServiceUpdateRequest request){
        com.booking_schedule.examine.entity.Service service = serviceRepository.findById(id).orElseThrow(()-> new AppException(ErrorCode.SERVICE_NOT_EXISTED));
        if(request.getName() != null && !request.getName().isBlank()){
            request.setName(request.getName().trim());
        }

        if (request.getCode() != null && !request.getCode().isBlank()) {
            if (serviceRepository.existsByCodeAndIdNot(request.getCode().trim(), id)) {
                throw new AppException(ErrorCode.SERVICE_CODE_EXISTED);
            }
            request.setCode(request.getCode().trim());
        }
        if (request.getSpecialtyId() != null) {
            service.setSpecialty(getActiveSpecialty(request.getSpecialtyId()));
        } else if (request.getStatus() == EStatus.ACTIVE && service.getSpecialty() != null
                && service.getSpecialty().getStatus() != EStatus.ACTIVE) {
            throw new AppException(ErrorCode.SPECIALTY_INACTIVE);
        }
        serviceMapper.updateService(service,request);
        service = saveService(service);
        return serviceMapper.toServiceResponse(service);
    }

    @Transactional
    public String deleteService(Long id){
        com.booking_schedule.examine.entity.Service service = serviceRepository.findById(id)
                .orElseThrow(()-> new AppException(ErrorCode.SERVICE_NOT_EXISTED));

        boolean hasAppointment = appointmentRepository.existsByServiceId(id);

        if(hasAppointment){
            if(service.getStatus() == EStatus.INACTIVE){
                throw new AppException(ErrorCode.SERVICE_ALREADY_INACTIVE);
            }
            service.setStatus(EStatus.INACTIVE);
            serviceRepository.save(service);
            return "Dịch vụ đang sử dụng đã được đặt trạng thái vô hiệu hóa.";
        }
        else{
            serviceRepository.delete(service);
            return "Dịch vụ đã được xóa thành công.";
        }
    }

    public ServiceResponse getServiceById(Long id) {
        com.booking_schedule.examine.entity.Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_EXISTED));
        return serviceMapper.toServiceResponse(service);
    }

    public List<ServiceResponse> getAllServices(EStatus status) {
        List<com.booking_schedule.examine.entity.Service> services;
        if (status != null) {
            services = serviceRepository.findByStatus(status);
        } else {
            services = serviceRepository.findAll();
        }
        return services.stream().map(serviceMapper::toServiceResponse).toList();
    }

    public Page<ServiceResponse> searchServices(String keyword, Long specialtyId, EStatus status, Pageable pageable) {
        Specification<com.booking_schedule.examine.entity.Service> filter = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            filter = filter.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (specialtyId != null) {
            filter = filter.and((root, query, cb) -> cb.equal(root.get("specialty").get("id"), specialtyId));
        }
        if (keyword != null && !keyword.isBlank()) {
            String escaped = keyword.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            String pattern = "%" + escaped + "%";
            filter = filter.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern, '\\'),
                    cb.like(cb.lower(root.get("code")), pattern, '\\')));
        }
        return serviceRepository.findAll(filter, pageable).map(serviceMapper::toServiceResponse);
    }

    public List<ServiceResponse> getPublicServices(Long specialtyId) {
        Specification<com.booking_schedule.examine.entity.Service> filter = (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), EStatus.ACTIVE),
                cb.equal(root.get("specialty").get("status"), EStatus.ACTIVE));
        if (specialtyId != null) {
            filter = filter.and((root, query, cb) -> cb.equal(root.get("specialty").get("id"), specialtyId));
        }
        return serviceRepository.findAll(filter, org.springframework.data.domain.Sort.by("id"))
                .stream().map(serviceMapper::toServiceResponse).toList();
    }

    private Specialty getActiveSpecialty(Long specialtyId) {
        Specialty specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_EXISTED));
        if (specialty.getStatus() != EStatus.ACTIVE) {
            throw new AppException(ErrorCode.SPECIALTY_INACTIVE);
        }
        return specialty;
    }

    private com.booking_schedule.examine.entity.Service saveService(com.booking_schedule.examine.entity.Service service) {
        try {
            // Flush để trả timestamp mới và bắt được tranh chấp mã ngay trong transaction.
            return serviceRepository.saveAndFlush(service);
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql
                        && (sql.getErrorCode() == 1062 || "23505".equals(sql.getSQLState()))) {
                    throw new AppException(ErrorCode.SERVICE_CODE_EXISTED);
                }
            }
            throw exception;
        }
    }

    private String generateCodeFromName(String name) {
        String temp = Normalizer.normalize(name, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noAccent = pattern.matcher(temp).replaceAll("").replace("đ", "d").replace("Đ", "D");
        String slug = noAccent.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "");

        String code = "DV-" + slug;
        if (code.length() > 45) {
            code = code.substring(0, 45); // Limit length to allow for numeric suffix
        }

        String finalCode = code;
        int count = 1;
        while (serviceRepository.existsByCode(finalCode)) {
            String suffix = String.valueOf(count);
            finalCode = code.substring(0, Math.min(code.length(), 50 - suffix.length())) + suffix;
            count++;
        }

        return finalCode;
    }
}
