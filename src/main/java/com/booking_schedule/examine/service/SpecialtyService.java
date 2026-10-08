package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.request.SpecialtyRequest;
import com.booking_schedule.examine.dto.request.SpecialtyUpdateRequest;
import com.booking_schedule.examine.dto.response.SpecialtyResponse;
import com.booking_schedule.examine.entity.Specialty;
import com.booking_schedule.examine.enums.EStatus;
import com.booking_schedule.examine.exception.AppException;
import com.booking_schedule.examine.exception.ErrorCode;
import com.booking_schedule.examine.mapper.SpecialtyMapper;
import com.booking_schedule.examine.repository.DoctorRepository;
import com.booking_schedule.examine.repository.RoomRepository;
import com.booking_schedule.examine.repository.ServiceRepository;
import com.booking_schedule.examine.repository.SpecialtyRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
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
            if (specialtyRepository.existsByCode(code.trim())) {
                throw new AppException(ErrorCode.SPECIALTY_CODE_EXISTED);
            }
        }

        Specialty specialty = specialtyMapper.toSpecialty(request);
        specialty.setCode(code.trim());
        specialty.setName(request.getName().trim());
        
        specialty = specialtyRepository.save(specialty);
        return specialtyMapper.toSpecialtyResponse(specialty);
    }

    @Transactional
    public SpecialtyResponse updateSpecialty(Long id, SpecialtyUpdateRequest request) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_EXISTED));

        // Kiểm tra trùng tên
        if(request.getName() != null && !request.getName().isBlank()) {
            if (specialtyRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
                throw new AppException(ErrorCode.SPECIALTY_NAME_EXISTED);
            }
            specialty.setName(request.getName().trim());
        }

        // Kiểm tra trùng mã (nếu có update mã)
        if (request.getCode() != null && !request.getCode().isBlank()) {
            if (specialtyRepository.existsByCodeAndIdNot(request.getCode().trim(), id)) {
                throw new AppException(ErrorCode.SPECIALTY_CODE_EXISTED);
            }
            specialty.setCode(request.getCode().trim());
        }

        specialtyMapper.updateSpecialty(specialty, request);

        specialty = specialtyRepository.save(specialty);
        return specialtyMapper.toSpecialtyResponse(specialty);
    }

    @Transactional
    public String deleteSpecialty(Long id) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_EXISTED));

        boolean hasRoom = roomRepository.existsBySpecialtyId(id);
        boolean hasService = serviceRepository.existsBySpecialtyId(id);
        boolean hasDoctor = doctorRepository.existsBySpecialtyId(id);

        if (hasRoom || hasService || hasDoctor) {
            if (specialty.getStatus() == EStatus.INACTIVE) {
                throw new AppException(ErrorCode.SPECIALTY_ALREADY_INACTIVE);
            }
            specialty.setStatus(EStatus.INACTIVE);
            specialtyRepository.save(specialty);
            return "Phòng khám, dịch vụ hoặc bác sĩ đang sử dụng chuyên khoa này. Chuyên khoa đã được đặt trạng thái vô hiệu hóa.";
        } else {
            specialtyRepository.delete(specialty);
            return "Chuyên khoa đã được xóa cứng thành công.";
        }
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
        String slug = noAccent.toUpperCase().replaceAll("[^A-Z0-9]+", "");
        
        String code = "CK-" + slug;
        if (code.length() > 45) {
            code = code.substring(0, 45); // Limit length to allow for numeric suffix
        }
        
        String finalCode = code;
        int count = 1;
        while (specialtyRepository.existsByCode(finalCode)) {
            finalCode = code + count;
            count++;
        }
        
        return finalCode;
    }
}
