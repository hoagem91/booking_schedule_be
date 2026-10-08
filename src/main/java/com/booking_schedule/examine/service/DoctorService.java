package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.request.DoctorCreateRequest;
import com.booking_schedule.examine.dto.request.DoctorUpdateRequest;
import com.booking_schedule.examine.dto.response.DoctorResponse;
import com.booking_schedule.examine.entity.Doctor;
import com.booking_schedule.examine.entity.Specialty;
import com.booking_schedule.examine.entity.User;
import com.booking_schedule.examine.enums.ERole;
import com.booking_schedule.examine.enums.EStatus;
import com.booking_schedule.examine.exception.AppException;
import com.booking_schedule.examine.exception.ErrorCode;
import com.booking_schedule.examine.mapper.DoctorMapper;
import com.booking_schedule.examine.repository.AppointmentRepository;
import com.booking_schedule.examine.repository.DoctorRepository;
import com.booking_schedule.examine.repository.DoctorScheduleRepository;
import com.booking_schedule.examine.repository.SpecialtyRepository;
import com.booking_schedule.examine.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DoctorService {

    DoctorMapper doctorMapper;
    DoctorRepository doctorRepository;
    UserRepository userRepository;
    SpecialtyRepository specialtyRepository;
    DoctorScheduleRepository doctorScheduleRepository;
    AppointmentRepository appointmentRepository;

    // ==================== CREATE ====================

    /**
     * Luồng nghiệp vụ tạo hồ sơ bác sĩ (Admin thực hiện):
     * 1. Kiểm tra User tồn tại
     * 2. Kiểm tra User chưa có hồ sơ bác sĩ (UNIQUE user_id)
     * 3. Kiểm tra User đã có role DOCTOR chưa
     * 4. Kiểm tra Specialty tồn tại và đang ACTIVE
     * 5. Kiểm tra doctorCode unique (hoặc tự sinh nếu không truyền)
     * 6. Build + lưu + trả về response
     */
    @Transactional
    public DoctorResponse createDoctor(DoctorCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (doctorRepository.existsByUserId(user.getId())) {
            throw new AppException(ErrorCode.DOCTOR_ALREADY_EXISTS);
        }

        boolean isDoctor = user.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.DOCTOR);
        if (!isDoctor) {
            throw new AppException(ErrorCode.USER_IS_NOT_DOCTOR);
        }

        Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_EXISTED));
        if (specialty.getStatus() != EStatus.ACTIVE) {
            throw new AppException(ErrorCode.SPECIALTY_INACTIVE);
        }

        String doctorCode = generateDoctorCode(request.getDoctorCode());

        Doctor doctor = Doctor.builder()
                .user(user)
                .specialty(specialty)
                .doctorCode(doctorCode)
                .position(request.getPosition())
                .experienceYears(request.getExperienceYears())
                .description(request.getDescription())
                .status(EStatus.ACTIVE)
                .build();

        doctor = doctorRepository.save(doctor);
        log.info("Created doctor profile for userId={}, doctorCode={}", user.getId(), doctorCode);
        return doctorMapper.toDoctorResponse(doctor);
    }

    // ==================== UPDATE ====================

    /**
     * Luồng nghiệp vụ cập nhật hồ sơ bác sĩ (Admin thực hiện):
     * 1. Kiểm tra Doctor tồn tại theo doctorId
     * 2. Nếu request đổi specialty → kiểm tra specialty mới hợp lệ & ACTIVE
     * 3. Nếu request đổi doctorCode → kiểm tra không trùng với bác sĩ khác
     * 4. Cập nhật các trường chuyên môn (position, experienceYears, description, status)
     * 5. Lưu và trả về response
     */
    @Transactional
    public DoctorResponse updateDoctor(Long doctorId, DoctorUpdateRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_EXISTED));

        if (request.getSpecialtyId() != null &&
                !request.getSpecialtyId().equals(doctor.getSpecialty().getId())) {
            Specialty newSpecialty = specialtyRepository.findById(request.getSpecialtyId())
                    .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_EXISTED));
            if (newSpecialty.getStatus() != com.booking_schedule.examine.enums.EStatus.ACTIVE) {
                throw new AppException(ErrorCode.SPECIALTY_INACTIVE);
            }
            doctor.setSpecialty(newSpecialty);
        }

        if (request.getDoctorCode() != null && !request.getDoctorCode().isBlank()
                && !request.getDoctorCode().equals(doctor.getDoctorCode())) {
            if (doctorRepository.existsByDoctorCodeAndIdNot(request.getDoctorCode(), doctorId)) {
                throw new AppException(ErrorCode.DOCTOR_ALREADY_EXISTS);
            }
            doctor.setDoctorCode(request.getDoctorCode());
        }

        doctorMapper.updateDoctorFromRequest(doctor, request);

        if (request.getStatus() != null) {
            doctor.setStatus(request.getStatus());
        }

        Doctor saved = doctorRepository.save(doctor);
        log.info("Updated doctor doctorId={}", doctorId);
        return doctorMapper.toDoctorResponse(saved);
    }

    // ==================== DELETE ====================

    /**
     * Luồng nghiệp vụ xóa / vô hiệu hóa bác sĩ (Admin thực hiện):
     * <p>
     * - Nếu bác sĩ ĐÃ có lịch làm việc (doctor_schedules) HOẶC lịch hẹn (appointments):
     * → SOFT DELETE: cập nhật status = 'INACTIVE' (bảo toàn lịch sử)
     * - Nếu bác sĩ CHƯA có bất kỳ lịch nào:
     * → HARD DELETE: xóa hẳn bản ghi khỏi database
     */
    @Transactional
    public String deleteDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_EXISTED));

        boolean hasSchedules = doctorScheduleRepository.existsByDoctorId(doctorId);
        boolean hasAppointments = appointmentRepository.existsByDoctorId(doctorId);

        if (hasSchedules || hasAppointments) {
            if (doctor.getStatus() == EStatus.INACTIVE) {
                throw new AppException(ErrorCode.DOCTOR_ALREADY_INACTIVE);
            }
            doctor.setStatus(EStatus.INACTIVE);
            doctorRepository.save(doctor);
            log.info("Soft-deleted (INACTIVE) doctor doctorId={}", doctorId);
            return "Bác sĩ đã được vô hiệu hóa (INACTIVE) do có lịch sử làm việc";
        } else {
            doctorRepository.delete(doctor);
            log.info("Hard-deleted doctor doctorId={}", doctorId);
            return "Bác sĩ đã được xóa vĩnh viễn";
        }
    }

    /**
     * Lấy chi tiết bác sĩ theo doctorId.
     */
    public DoctorResponse getDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_EXISTED));
        return doctorMapper.toDoctorResponse(doctor);
    }

    /**
     * Lấy danh sách bác sĩ, hỗ trợ lọc theo specialtyId và/hoặc status.
     *
     * @param specialtyId null = không lọc theo chuyên khoa
     * @param status      null = trả về tất cả trạng thái
     */
    public List<DoctorResponse> getAllDoctors(Long specialtyId, EStatus status) {
        List<Doctor> doctors;

        if (specialtyId != null && status != null) {
            doctors = doctorRepository.findBySpecialtyIdAndStatus(specialtyId, status);
        } else if (specialtyId != null) {
            doctors = doctorRepository.findBySpecialtyId(specialtyId);
        } else if (status != null) {
            doctors = doctorRepository.findByStatus(status);
        } else {
            doctors = doctorRepository.findAll();
        }

        return doctors.stream().map(doctorMapper::toDoctorResponse).toList();
    }


    /**
     * Sinh mã bác sĩ tự động nếu client không truyền.
     * Format: BS-YYYYMMDD-<5 chữ số cuối của millis>
     */
    private String generateDoctorCode(String requestedCode) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            if (doctorRepository.existsByDoctorCode(requestedCode)) {
                throw new AppException(ErrorCode.DOCTOR_ALREADY_EXISTS);
            }
            return requestedCode;
        }
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = String.valueOf(System.currentTimeMillis() % 100000);
        return "BS-" + date + "-" + suffix;
    }
}
