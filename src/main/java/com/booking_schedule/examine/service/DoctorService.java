package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.request.*;
import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.entity.*;
import com.booking_schedule.examine.enums.*;
import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.mapper.DoctorMapper;
import com.booking_schedule.examine.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorService {
    private final DoctorMapper doctorMapper;
    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final DoctorScheduleRepository schedules;
    private final DoctorAuditRepository audits;
    private final UserService users;
    private final EntityManager entityManager;

    @Transactional
    public DoctorResponse createDoctor(DoctorCreateRequest request) {
        if (!request.isAccountSelectionValid()) invalid();
        User actor = users.beginProfileAdminWrite();
        Long userId = request.getUserId();
        if (userId == null) userId = users.createUser(request.getUser()).getId();
        User target = users.lockProfileTarget(actor, userId);
        if (!"ACTIVE".equals(target.getStatus())) throw new AppException(ErrorCode.USER_INACTIVE);
        if (doctorRepository.findLockedByUserId(userId).isPresent()) throw new AppException(ErrorCode.DOCTOR_ALREADY_EXISTS);
        Specialty specialty = activeSpecialty(request.getSpecialtyId());
        String code = request.getDoctorCode() == null || request.getDoctorCode().isBlank()
                ? ProfileCodeGenerator.generate("BS") : normalizeCode(request.getDoctorCode());
        ensureUniqueCode(code, null);
        validateExperience(request.getExperienceYears(), true);
        Doctor doctor = Doctor.builder().user(target).specialty(specialty).doctorCode(code).status(EStatus.ACTIVE)
                .position(text(request.getPosition(), 100, true)).experienceYears(request.getExperienceYears())
                .description(text(request.getDescription(), 2000, false)).build();
        doctorRepository.saveAndFlush(doctor);
        target.setDoctor(doctor);
        users.linkDoctorRole(actor, target);
        audit(actor, doctor, "CREATE", fields(doctor).keySet());
        return response(doctor, users.profileReadContext());
    }

    @Transactional
    public DoctorResponse updateDoctor(Long doctorId, DoctorUpdateRequest request) {
        User actor = users.beginProfileAdminWrite();
        Doctor doctor = lockedTarget(actor, doctorId);
        Map<String, Object> before = fields(doctor);
        if (!request.isClearSelectionValid()) invalid();
        validateExperience(request.getExperienceYears(), false);
        if (request.getSpecialtyId() != null && !request.getSpecialtyId().equals(doctor.getSpecialty().getId())) {
            requireNoSchedules(doctor, List.of(EStatusDoctorSchedule.OPEN, EStatusDoctorSchedule.FULL,
                    EStatusDoctorSchedule.CLOSED), ErrorCode.DOCTOR_SPECIALTY_CHANGE_NOT_ALLOWED);
            doctor.setSpecialty(activeSpecialty(request.getSpecialtyId()));
        }
        if (request.getDoctorCode() != null) {
            String code = normalizeCode(request.getDoctorCode());
            ensureUniqueCode(code, doctorId);
            doctor.setDoctorCode(code);
        }
        if (request.getPosition() != null) doctor.setPosition(text(request.getPosition(), 100, true));
        if (request.getExperienceYears() != null) doctor.setExperienceYears(request.getExperienceYears());
        if (Boolean.TRUE.equals(request.getClearDescription())) doctor.setDescription(null);
        else if (request.getDescription() != null) doctor.setDescription(text(request.getDescription(), 2000, false));
        if (request.getStatus() != null) applyStatus(doctor, request.getStatus());
        if (request.getAccount() != null) {
            var account = request.getAccount();
            users.updateUser(doctor.getUser().getId(), UpdateUserRequest.builder().fullName(account.getFullName())
                    .email(account.getEmail()).phone(account.getPhone()).build());
        }
        doctorRepository.saveAndFlush(doctor);
        audit(actor, doctor, "UPDATE", changedFields(before, fields(doctor)));
        return response(doctor, users.profileReadContext());
    }

    @Transactional
    public DoctorResponse changeStatus(Long doctorId, EStatus status) {
        User actor = users.beginProfileAdminWrite();
        Doctor doctor = lockedTarget(actor, doctorId);
        var before = fields(doctor);
        applyStatus(doctor, status);
        doctorRepository.saveAndFlush(doctor);
        audit(actor, doctor, "STATUS_CHANGED", changedFields(before, fields(doctor)));
        return response(doctor, users.profileReadContext());
    }

    @Transactional
    public DoctorDeleteResponse deleteDoctor(Long doctorId) {
        User actor = users.beginProfileAdminWrite();
        Doctor doctor = lockedTarget(actor, doctorId);
        var before = fields(doctor);
        applyStatus(doctor, EStatus.INACTIVE);
        doctorRepository.saveAndFlush(doctor);
        audit(actor, doctor, "DEACTIVATED", changedFields(before, fields(doctor)));
        return new DoctorDeleteResponse(doctorId, "DEACTIVATED", "INACTIVE",
                "Bác sĩ đã được vô hiệu hóa; hồ sơ và lịch sử được giữ lại");
    }

    public DoctorResponse getDoctor(Long doctorId) {
        var context = users.profileReadContext();
        return response(doctorRepository.findById(doctorId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_EXISTED)), context);
    }

    public List<DoctorResponse> getAllDoctors(Long specialtyId, EStatus status) {
        var context = users.profileReadContext();
        return doctorRepository.findAll(filters(DoctorSearchRequest.builder().specialtyId(specialtyId).status(status).build()),
                Sort.by("id")).stream().map(d -> response(d, context)).toList();
    }

    public Page<DoctorResponse> search(DoctorSearchRequest request, Pageable pageable) {
        var context = users.profileReadContext();
        return doctorRepository.findAll(filters(request), pageable).map(d -> response(d, context));
    }

    public Page<DoctorAuditResponse> auditLogs(Long doctorId, Pageable pageable) {
        users.profileReadContext();
        if (!doctorRepository.existsById(doctorId)) throw new AppException(ErrorCode.DOCTOR_NOT_EXISTED);
        return audits.findByDoctorId(doctorId, pageable).map(row -> new DoctorAuditResponse(row.getId(),
                row.getActorUserId(), row.getDoctorId(), row.getAction(),
                row.getChangedFields().isEmpty() ? List.of() : List.of(row.getChangedFields().split(",")), row.getCreatedAt()));
    }

    private Doctor lockedTarget(User actor, Long doctorId) {
        Long userId = doctorRepository.findUserIdByDoctorId(doctorId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_EXISTED));
        users.lockProfileTarget(actor, userId);
        Doctor doctor = doctorRepository.findLockedById(doctorId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_EXISTED));
        entityManager.refresh(doctor, LockModeType.PESSIMISTIC_WRITE);
        return doctor;
    }

    private void applyStatus(Doctor doctor, EStatus status) {
        if (status == null) invalid();
        if (status == EStatus.ACTIVE) {
            if (!"ACTIVE".equals(doctor.getUser().getStatus())) throw new AppException(ErrorCode.USER_INACTIVE);
            activeSpecialty(doctor.getSpecialty().getId());
        } else {
            requireNoSchedules(doctor, List.of(EStatusDoctorSchedule.OPEN, EStatusDoctorSchedule.FULL),
                    ErrorCode.DOCTOR_HAS_FUTURE_SCHEDULES);
        }
        doctor.setStatus(status);
    }

    private void requireNoSchedules(Doctor doctor, List<EStatusDoctorSchedule> statuses, ErrorCode error) {
        LocalDateTime now = LocalDateTime.now();
        if (!schedules.findLockedUnfinished(doctor.getId(), now.toLocalDate(), now.toLocalTime(), statuses).isEmpty())
            throw new AppException(error);
    }

    private Specialty activeSpecialty(Long id) {
        if (id == null || id <= 0) { invalid(); }
        Specialty specialty = specialtyRepository.findLockedById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_EXISTED));
        entityManager.refresh(specialty, LockModeType.PESSIMISTIC_WRITE);
        if (specialty.getStatus() != EStatus.ACTIVE) throw new AppException(ErrorCode.SPECIALTY_INACTIVE);
        return specialty;
    }

    private Specification<Doctor> filters(DoctorSearchRequest request) {
        if ((request.getSpecialtyId() != null && request.getSpecialtyId() <= 0)
                || (request.getUserStatus() != null && !Set.of("ACTIVE", "INACTIVE").contains(request.getUserStatus()))) invalid();
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (request.getSpecialtyId() != null) predicates.add(cb.equal(root.get("specialty").get("id"), request.getSpecialtyId()));
            if (request.getStatus() != null) predicates.add(cb.equal(root.get("status"), request.getStatus()));
            if (request.getUserStatus() != null) predicates.add(cb.equal(root.get("user").get("status"), request.getUserStatus()));
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String pattern = "%" + request.getKeyword().trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("doctorCode")), pattern, '\\'),
                        cb.like(cb.lower(root.get("user").get("username")), pattern, '\\'),
                        cb.like(cb.lower(root.get("user").get("fullName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("user").get("email")), pattern, '\\'),
                        cb.like(cb.lower(root.get("user").get("phone")), pattern, '\\')));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private DoctorResponse response(Doctor doctor, UserService.ProfileReadContext context) {
        DoctorResponse response = doctorMapper.toDoctorResponse(doctor);
        var capability = users.profileCapabilities(doctor.getUser(), context.actor(), context.activeSuperAdmins());
        // Capabilities describe authorization; schedule conflicts remain authoritative server checks.
        response.setCapabilities(new DoctorResponse.Capabilities(capability.canEdit(), capability.canEdit(),
                capability.canEdit(), capability.canEdit(), capability.canDeactivate(), false));
        return response;
    }

    private String normalizeCode(String value) { return text(text(value, 50, true).toUpperCase(Locale.ROOT), 50, true); }
    private void ensureUniqueCode(String code, Long id) {
        if (id == null ? doctorRepository.existsByDoctorCodeIgnoreCase(code)
                : doctorRepository.existsByDoctorCodeIgnoreCaseAndIdNot(code, id))
            throw new AppException(ErrorCode.DOCTOR_CODE_EXISTS);
    }
    private String text(String value, int max, boolean required) {
        String result = value == null ? null : value.trim();
        if ((required && (result == null || result.isEmpty())) || (result != null && result.length() > max)) invalid();
        return result == null || result.isEmpty() ? null : result;
    }
    private void validateExperience(Integer years, boolean required) {
        if ((required && years == null) || (years != null && (years < 0 || years > 70))) invalid();
    }
    private void invalid() { throw new AppException(ErrorCode.DOCTOR_INVALID_REQUEST); }

    private Map<String, Object> fields(Doctor doctor) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("doctorCode", doctor.getDoctorCode());
        fields.put("specialtyId", doctor.getSpecialty().getId());
        fields.put("position", doctor.getPosition());
        fields.put("experienceYears", doctor.getExperienceYears());
        fields.put("description", doctor.getDescription());
        fields.put("status", doctor.getStatus());
        fields.put("fullName", doctor.getUser().getFullName());
        fields.put("email", doctor.getUser().getEmail());
        fields.put("phone", doctor.getUser().getPhone());
        return fields;
    }
    private Set<String> changedFields(Map<String, Object> before, Map<String, Object> after) {
        Set<String> result = new TreeSet<>();
        before.forEach((key, value) -> { if (!Objects.equals(value, after.get(key))) result.add(key); });
        return result;
    }
    private void audit(User actor, Doctor doctor, String action, Set<String> fields) {
        audits.save(DoctorAudit.builder().actorUserId(actor.getId()).doctorId(doctor.getId()).action(action)
                .changedFields(String.join(",", new TreeSet<>(fields))).build());
    }
}
