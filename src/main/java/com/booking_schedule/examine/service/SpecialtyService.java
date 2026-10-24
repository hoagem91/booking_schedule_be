package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.request.SpecialtyRequest;
import com.booking_schedule.examine.dto.request.SpecialtyUpdateRequest;
import com.booking_schedule.examine.dto.response.SpecialtyResponse;
import com.booking_schedule.examine.dto.response.SpecialtyDeleteResponse;
import com.booking_schedule.examine.entity.Specialty;
import com.booking_schedule.examine.enums.EStatus;
import com.booking_schedule.examine.enums.EStatusDoctorSchedule;
import com.booking_schedule.examine.exception.AppException;
import com.booking_schedule.examine.exception.ErrorCode;
import com.booking_schedule.examine.mapper.SpecialtyMapper;
import com.booking_schedule.examine.repository.DoctorRepository;
import com.booking_schedule.examine.repository.RoomRepository;
import com.booking_schedule.examine.repository.ServiceRepository;
import com.booking_schedule.examine.repository.SpecialtyRepository;
import com.booking_schedule.examine.repository.DoctorScheduleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SpecialtyService {
    SpecialtyRepository specialtyRepository;
    SpecialtyMapper specialtyMapper;
    RoomRepository roomRepository;
    ServiceRepository serviceRepository;
    DoctorRepository doctorRepository;
    DoctorScheduleRepository scheduleRepository;

    @Transactional
    public SpecialtyResponse createSpecialty(SpecialtyRequest request) {
        if (specialtyRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new AppException(ErrorCode.SPECIALTY_NAME_EXISTED);
        }

        // Xử lý mã chuyên khoa
        String code = request.getCode();
        if (code == null || code.trim().isEmpty()) {
            code = generateCodeFromName(request.getName().trim());
        } else {
            if (specialtyRepository.existsByCodeIgnoreCase(normalizeCode(code))) {
                throw new AppException(ErrorCode.SPECIALTY_CODE_EXISTED);
            }
        }

        Specialty specialty = specialtyMapper.toSpecialty(request);
        specialty.setCode(normalizeCode(code));
        specialty.setName(request.getName().trim());
        
        specialty = specialtyRepository.save(specialty);
        return specialtyMapper.toSpecialtyResponse(specialty);
    }

    @Transactional
    public SpecialtyResponse updateSpecialty(Long id, SpecialtyUpdateRequest request) {
        Specialty specialty = locked(id);

        // Kiểm tra trùng tên
        if(request.getName() != null && !request.getName().isBlank()) {
            if (specialtyRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
                throw new AppException(ErrorCode.SPECIALTY_NAME_EXISTED);
            }
            specialty.setName(request.getName().trim());
        }

        // Kiểm tra trùng mã (nếu có update mã)
        if (request.getCode() != null && !request.getCode().isBlank()) {
            if (specialtyRepository.existsByCodeIgnoreCaseAndIdNot(normalizeCode(request.getCode()), id)) {
                throw new AppException(ErrorCode.SPECIALTY_CODE_EXISTED);
            }
            specialty.setCode(normalizeCode(request.getCode()));
        }

        // Apply only validated, normalized values; do not overwrite them with the raw DTO.
        if (request.getDescription() != null) specialty.setDescription(request.getDescription());
        if (request.getStatus() != null) applyStatus(specialty, request.getStatus());

        specialty = specialtyRepository.save(specialty);
        return specialtyMapper.toSpecialtyResponse(specialty);
    }

    @Transactional
    public SpecialtyDeleteResponse deleteSpecialty(Long id) {
        Specialty specialty = locked(id);

        boolean hasRoom = roomRepository.existsBySpecialtyId(id);
        boolean hasService = serviceRepository.existsBySpecialtyId(id);
        boolean hasDoctor = doctorRepository.existsBySpecialtyId(id);

        if (hasRoom || hasService || hasDoctor) {
            if (specialty.getStatus() == EStatus.INACTIVE) {
                throw new AppException(ErrorCode.SPECIALTY_ALREADY_INACTIVE);
            }
            applyStatus(specialty, EStatus.INACTIVE);
            specialtyRepository.save(specialty);
            return new SpecialtyDeleteResponse(id, SpecialtyDeleteResponse.Action.DEACTIVATED, EStatus.INACTIVE,
                    "Chuyên khoa đã được vô hiệu hóa; giữ nguyên phòng khám, dịch vụ và bác sĩ liên quan.");
        } else {
            specialtyRepository.delete(specialty);
            return new SpecialtyDeleteResponse(id, SpecialtyDeleteResponse.Action.DELETED, null,
                    "Chuyên khoa đã được xóa cứng thành công.");
        }
    }

    @Transactional
    public SpecialtyResponse updateStatus(Long id, EStatus status) {
        Specialty specialty = locked(id);
        applyStatus(specialty, status);
        return specialtyMapper.toSpecialtyResponse(specialtyRepository.save(specialty));
    }

    @Transactional(readOnly = true)
    public Page<SpecialtyResponse> search(String keyword, EStatus status, Pageable pageable) {
        Specification<Specialty> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((root, query, cb) -> cb.or(cb.like(cb.lower(root.get("code")), pattern, '\\'),
                    cb.like(cb.lower(root.get("name")), pattern, '\\')));
        }
        return specialtyRepository.findAll(spec, pageable).map(specialtyMapper::toSpecialtyResponse);
    }

    private Specialty locked(Long id) {
        return specialtyRepository.findLockedById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_EXISTED));
    }

    private void applyStatus(Specialty specialty, EStatus status) {
        if (status == null) throw new AppException(ErrorCode.SPECIALTY_INVALID_REQUEST);
        if (status == EStatus.INACTIVE && scheduleRepository.existsActiveSchedulesForSpecialty(
                specialty.getId(), LocalDate.now(), List.of(EStatusDoctorSchedule.OPEN, EStatusDoctorSchedule.FULL)))
            throw new AppException(ErrorCode.SPECIALTY_HAS_FUTURE_SCHEDULES);
        specialty.setStatus(status);
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    public SpecialtyResponse getSpecialtyById(Long id) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_EXISTED));
        return specialtyMapper.toSpecialtyResponse(specialty);
    }

    public List<SpecialtyResponse> getAllSpecialties(EStatus status) {
        List<Specialty> specialties;
        if (status != null) {
            specialties = specialtyRepository.findByStatus(status);
        } else {
            specialties = specialtyRepository.findAll();
        }
        return specialties.stream().map(specialtyMapper::toSpecialtyResponse).toList();
    }

    private String generateCodeFromName(String name) {
        String temp = Normalizer.normalize(name, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noAccent = pattern.matcher(temp).replaceAll("").replace("đ", "d").replace("Đ", "D");
        String slug = noAccent.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "");
        
        String code = "CK-" + slug;
        if (code.length() > 45) {
            code = code.substring(0, 45); // Limit length to allow for numeric suffix
        }
        
        String finalCode = code;
        int count = 1;
        while (specialtyRepository.existsByCodeIgnoreCase(finalCode)) {
            finalCode = code + count;
            count++;
        }
        
        return finalCode;
    }
}
