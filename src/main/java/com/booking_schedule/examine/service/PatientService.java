package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.request.PatientCreateRequest;
import com.booking_schedule.examine.dto.request.PatientUpdateRequest;
import com.booking_schedule.examine.dto.response.PatientResponse;
import com.booking_schedule.examine.entity.Patient;
import com.booking_schedule.examine.entity.User;
import com.booking_schedule.examine.enums.ERole;
import com.booking_schedule.examine.exception.AppException;
import com.booking_schedule.examine.exception.ErrorCode;
import com.booking_schedule.examine.mapper.PatientMapper;
import com.booking_schedule.examine.repository.PatientRepository;
import com.booking_schedule.examine.repository.RoleRepository;
import com.booking_schedule.examine.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional(readOnly = true)
public class PatientService {

    UserRepository userRepository;
    PatientRepository patientRepository;
    PatientMapper patientMapper;
    RoleRepository roleRepository;

    /**
     * Tạo hồ sơ bệnh nhân cho người dùng đang đăng nhập.
     *
     * Luồng nghiệp vụ:
     * 1. Lấy username từ SecurityContext (JWT đã được xác thực bởi Spring Security)
     * 2. Query user từ DB theo username đó
     * 3. Kiểm tra user đã có hồ sơ bệnh nhân chưa (1 user - 1 patient)
     * 4. Kiểm tra vai trò PATIENT (tài khoản cũ) hoặc DOCTOR (bác sĩ tự đăng ký khám)
     * 5. Tự sinh mã, tạo hồ sơ, bổ sung PATIENT và thu hồi JWT cũ
     * 6. Lưu và trả về PatientResponse
     */
    @Transactional
    public PatientResponse createPatient(PatientCreateRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        currentUser = userRepository.findLockedById(currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (!"ACTIVE".equals(currentUser.getStatus())) throw new AppException(ErrorCode.USER_INACTIVE);

        if (patientRepository.existsByUserId(currentUser.getId())) {
            throw new AppException(ErrorCode.PATIENT_ALREADY_EXISTS);
        }

        boolean isPatient = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.PATIENT || role.getName() == ERole.DOCTOR);
        if (!isPatient) {
            throw new AppException(ErrorCode.USER_IS_NOT_PATIENT);
        }

        Patient patient = Patient.builder()
                .user(currentUser)
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .healthInsuranceCode(request.getHealthInsuranceCode())
                .patientCode(ProfileCodeGenerator.generate("BN"))
                .build();

        Patient savedPatient = patientRepository.save(patient);
        currentUser.setPatient(savedPatient);
        if (currentUser.getRoles().stream().noneMatch(role -> role.getName() == ERole.PATIENT)) {
            currentUser.getRoles().add(roleRepository.findByName(ERole.PATIENT)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_ROLE)));
        }
        currentUser.revokeTokens();
        log.info("Created patient profile for user: {}", username);
        return patientMapper.toPatientResponse(savedPatient);
    }

    /**
     * Cập nhật hồ sơ bệnh nhân của người dùng đang đăng nhập.
     * User chỉ có thể cập nhật hồ sơ của chính mình.
     */
    @Transactional
    public PatientResponse updateMyPatient(PatientUpdateRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Patient patient = patientRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));

        patientMapper.updatePatient(patient, request);
        return patientMapper.toPatientResponse(patientRepository.save(patient));
    }

    /**
     * Admin cập nhật hồ sơ bệnh nhân theo userId (dùng cho màn hình admin).
     */
    @Transactional
    public PatientResponse updatePatientByUserId(Long userId, PatientUpdateRequest request) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));

        patientMapper.updatePatient(patient, request);
        return patientMapper.toPatientResponse(patientRepository.save(patient));
    }

    /**
     * Giữ hồ sơ bệnh nhân. Admin khóa User nếu cần ngừng tài khoản.
     */
    public void deletePatient(Long userId) {
        patientRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
        throw new AppException(ErrorCode.PATIENT_DELETE_NOT_ALLOWED);
    }

    /**
     * Lấy danh sách tất cả bệnh nhân (Admin).
     */
    public List<PatientResponse> getAllPatient() {
        return patientRepository.findAll()
                .stream()
                .map(patientMapper::toPatientResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy hồ sơ bệnh nhân của người dùng đang đăng nhập (user tự xem).
     */
    public PatientResponse getMyPatient() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return patientMapper.toPatientResponse(
                patientRepository.findByUserId(currentUser.getId())
                        .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND))
        );
    }

    /**
     * Lấy hồ sơ bệnh nhân theo userId (Admin).
     */
    public PatientResponse getPatientByUserId(Long userId) {
        return patientMapper.toPatientResponse(
                patientRepository.findByUserId(userId)
                        .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND))
        );
    }

}
