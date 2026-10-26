package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.request.*;
import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.entity.*;
import com.booking_schedule.examine.enums.ERole;
import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.mapper.PatientMapper;
import com.booking_schedule.examine.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final RoleRepository roleRepository;
    private final PatientAuditRepository audits;
    private final UserService users;
    private final EntityManager entityManager;

    @Transactional
    public PatientResponse createAdminPatient(AdminPatientCreateRequest request) {
        if (!request.isAccountSelectionValid()) throw new AppException(ErrorCode.PATIENT_INVALID_REQUEST);
        validateCreate(request);
        User actor = users.beginProfileAdminWrite();
        Long userId = request.getUserId();
        if (userId == null) userId = users.createUser(request.getUser()).getId();
        User target = users.lockProfileTarget(actor, userId);
        if (!"ACTIVE".equals(target.getStatus())) throw new AppException(ErrorCode.USER_INACTIVE);
        Patient patient = createProfile(target, request);
        users.linkPatientRole(actor, target);
        audit(actor, patient, "CREATE", populatedFields(patient));
        return adminResponse(patient, users.profileReadContext());
    }

    @Transactional
    public PatientResponse updateAdminPatient(Long patientId, AdminPatientUpdateRequest request) {
        User actor = users.beginProfileAdminWrite();
        Long userId = patientRepository.findUserIdByPatientId(patientId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
        users.lockProfileTarget(actor, userId);
        Patient patient = patientRepository.findLockedById(patientId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
        Map<String, Object> before = fields(patient);
        apply(patient, request);
        if (request.getAccount() != null) {
            var account = request.getAccount();
            users.updateUser(userId, UpdateUserRequest.builder().fullName(account.getFullName())
                    .email(account.getEmail()).phone(account.getPhone()).build());
        }
        patientRepository.saveAndFlush(patient);
        audit(actor, patient, "UPDATE", changedFields(before, fields(patient)));
        return adminResponse(patient, users.profileReadContext());
    }

    public PatientResponse getAdminPatient(Long patientId) {
        var context = users.profileReadContext();
        Patient patient = patientRepository.findById(patientId).orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
        return adminResponse(patient, context);
    }

    public Page<PatientResponse> search(PatientSearchRequest request, Pageable pageable) {
        var context = users.profileReadContext();
        if (!request.isDateRangeValid()
                || (request.getDateOfBirthFrom() != null && request.getDateOfBirthFrom().isAfter(LocalDate.now()))
                || (request.getDateOfBirthTo() != null && request.getDateOfBirthTo().isAfter(LocalDate.now()))
                || (request.getUserStatus() != null && !Set.of("ACTIVE", "INACTIVE").contains(request.getUserStatus())))
            throw new AppException(ErrorCode.PATIENT_INVALID_REQUEST);
        Specification<Patient> spec = (root, query, cb) -> cb.conjunction();
        if (request.getGender() != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("gender"), request.getGender()));
        if (request.getUserStatus() != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("user").get("status"), request.getUserStatus()));
        if (request.getDateOfBirthFrom() != null) spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("dateOfBirth"), request.getDateOfBirthFrom()));
        if (request.getDateOfBirthTo() != null) spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("dateOfBirth"), request.getDateOfBirthTo()));
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String pattern = "%" + request.getKeyword().trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((r, q, cb) -> cb.or(cb.like(cb.lower(r.get("patientCode")), pattern, '\\'),
                    cb.like(cb.lower(r.get("user").get("username")), pattern, '\\'),
                    cb.like(cb.lower(r.get("user").get("fullName")), pattern, '\\'),
                    cb.like(cb.lower(r.get("user").get("email")), pattern, '\\'),
                    cb.like(cb.lower(r.get("user").get("phone")), pattern, '\\')));
        }
        return patientRepository.findAll(spec, pageable).map(patient -> adminResponse(patient, context));
    }

    public Page<PatientAuditResponse> auditLogs(Long patientId, Pageable pageable) {
        users.profileReadContext();
        if (!patientRepository.existsById(patientId)) throw new AppException(ErrorCode.PATIENT_NOT_FOUND);
        return audits.findByPatientId(patientId, pageable).map(row -> new PatientAuditResponse(row.getId(),
                row.getActorUserId(), row.getPatientId(), row.getAction(),
                row.getChangedFields().isEmpty() ? List.of() : List.of(row.getChangedFields().split(",")), row.getCreatedAt()));
    }

    public void deleteAdminPatient(Long patientId) {
        users.profileReadContext();
        if (!patientRepository.existsById(patientId)) throw new AppException(ErrorCode.PATIENT_NOT_FOUND);
        throw new AppException(ErrorCode.PATIENT_DELETE_NOT_ALLOWED);
    }

    /** Legacy endpoints continue to interpret their path ID as userId. */
    @Transactional
    public PatientResponse updatePatientByUserId(Long userId, PatientUpdateRequest request) {
        User actor = users.beginProfileAdminWrite();
        users.lockProfileTarget(actor, userId);
        Patient patient = patientRepository.findLockedByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
        Map<String, Object> before = fields(patient);
        apply(patient, request);
        patientRepository.saveAndFlush(patient);
        audit(actor, patient, "UPDATE", changedFields(before, fields(patient)));
        return adminResponse(patient, users.profileReadContext());
    }

    public PatientResponse getPatientByUserId(Long userId) {
        var context = users.profileReadContext();
        return adminResponse(patientRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND)), context);
    }

    public List<PatientResponse> getAllPatient() {
        var context = users.profileReadContext();
        return patientRepository.findAll().stream().map(patient -> adminResponse(patient, context)).toList();
    }

    public void deletePatient(Long userId) {
        users.profileReadContext();
        patientRepository.findByUserId(userId).orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
        throw new AppException(ErrorCode.PATIENT_DELETE_NOT_ALLOWED);
    }

    @Transactional
    public PatientResponse createPatient(PatientCreateRequest request) {
        User user = currentUser(true);
        if (user.getRoles().stream().noneMatch(role -> role.getName() == ERole.PATIENT || role.getName() == ERole.DOCTOR))
            throw new AppException(ErrorCode.USER_IS_NOT_PATIENT);
        Patient patient = createProfile(user, request);
        user.getRoles().add(roleRepository.findByName(ERole.PATIENT).orElseThrow(() -> new AppException(ErrorCode.INVALID_ROLE)));
        user.revokeTokens();
        userRepository.saveAndFlush(user);
        audit(user, patient, "SELF_CREATE", populatedFields(patient));
        return patientMapper.toPatientResponse(patient);
    }

    @Transactional
    public PatientResponse updateMyPatient(PatientUpdateRequest request) {
        User user = currentUser(true);
        requirePatient(user);
        Patient patient = patientRepository.findLockedByUserId(user.getId()).orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
        Map<String, Object> before = fields(patient);
        apply(patient, request);
        patientRepository.saveAndFlush(patient);
        audit(user, patient, "SELF_UPDATE", changedFields(before, fields(patient)));
        return patientMapper.toPatientResponse(patient);
    }

    public PatientResponse getMyPatient() {
        User user = currentUser(false);
        requirePatient(user);
        return patientMapper.toPatientResponse(patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND)));
    }

    private Patient createProfile(User user, PatientCreateRequest request) {
        validateCreate(request);
        // Current locking read also sees profiles created while waiting for the User lock on MySQL.
        if (patientRepository.findLockedByUserId(user.getId()).isPresent()) throw new AppException(ErrorCode.PATIENT_ALREADY_EXISTS);
        Patient patient = Patient.builder().user(user).patientCode(ProfileCodeGenerator.generate("BN"))
                .dateOfBirth(request.getDateOfBirth()).gender(request.getGender())
                .address(optionalText(request.getAddress(), 500)).healthInsuranceCode(optionalText(request.getHealthInsuranceCode(), 100)).build();
        patientRepository.saveAndFlush(patient);
        user.setPatient(patient);
        return patient;
    }

    private void validateCreate(PatientCreateRequest request) {
        if (request.getDateOfBirth() == null || request.getDateOfBirth().isAfter(LocalDate.now()) || request.getGender() == null)
            throw new AppException(ErrorCode.PATIENT_INVALID_REQUEST);
    }

    private void apply(Patient patient, PatientUpdateRequest request) {
        if (!request.isClearSelectionValid() || (request.getDateOfBirth() != null && request.getDateOfBirth().isAfter(LocalDate.now())))
            throw new AppException(ErrorCode.PATIENT_INVALID_REQUEST);
        if (request.getDateOfBirth() != null) patient.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) patient.setGender(request.getGender());
        if (Boolean.TRUE.equals(request.getClearAddress())) patient.setAddress(null);
        else if (request.getAddress() != null) patient.setAddress(requiredText(request.getAddress(), 500));
        if (Boolean.TRUE.equals(request.getClearHealthInsuranceCode())) patient.setHealthInsuranceCode(null);
        else if (request.getHealthInsuranceCode() != null) patient.setHealthInsuranceCode(requiredText(request.getHealthInsuranceCode(), 100));
    }

    private String optionalText(String value, int max) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.length() > max) throw new AppException(ErrorCode.PATIENT_INVALID_REQUEST);
        return normalized.isEmpty() ? null : normalized;
    }
    private String requiredText(String value, int max) {
        String normalized = optionalText(value, max);
        if (normalized == null) throw new AppException(ErrorCode.PATIENT_INVALID_REQUEST);
        return normalized;
    }

    private User currentUser(boolean lock) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AppException(ErrorCode.UNAUTHENTICATED);
        User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (lock) {
            user = userRepository.findLockedById(user.getId()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        }
        if (!"ACTIVE".equals(user.getStatus())) throw new AppException(ErrorCode.USER_INACTIVE);
        return user;
    }

    private void requirePatient(User user) {
        if (user.getRoles().stream().noneMatch(role -> role.getName() == ERole.PATIENT)) throw new AppException(ErrorCode.USER_IS_NOT_PATIENT);
    }

    private PatientResponse adminResponse(Patient patient, UserService.ProfileReadContext context) {
        PatientResponse response = patientMapper.toPatientResponse(patient);
        var capability = users.profileCapabilities(patient.getUser(), context.actor(), context.activeSuperAdmins());
        response.setCapabilities(new PatientResponse.Capabilities(capability.canEdit(), capability.canEdit(),
                capability.canDeactivate(), false));
        return response;
    }

    private Map<String, Object> fields(Patient patient) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("dateOfBirth", patient.getDateOfBirth());
        fields.put("gender", patient.getGender());
        fields.put("address", patient.getAddress());
        fields.put("healthInsuranceCode", patient.getHealthInsuranceCode());
        fields.put("fullName", patient.getUser().getFullName());
        fields.put("email", patient.getUser().getEmail());
        fields.put("phone", patient.getUser().getPhone());
        return fields;
    }
    private Set<String> populatedFields(Patient patient) {
        Set<String> result = new TreeSet<>();
        fields(patient).forEach((field, value) -> { if (value != null) result.add(field); });
        return result;
    }
    private Set<String> changedFields(Map<String, Object> before, Map<String, Object> after) {
        Set<String> result = new TreeSet<>();
        before.forEach((field, value) -> { if (!Objects.equals(value, after.get(field))) result.add(field); });
        return result;
    }
    private void audit(User actor, Patient patient, String action, Set<String> changedFields) {
        audits.save(PatientAudit.builder().actorUserId(actor.getId()).patientId(patient.getId()).action(action)
                .changedFields(String.join(",", changedFields)).build());
    }
}
