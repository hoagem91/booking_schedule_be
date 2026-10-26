package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.request.*;
import com.booking_schedule.examine.dto.response.*;
import com.booking_schedule.examine.entity.*;
import com.booking_schedule.examine.enums.ERole;
import com.booking_schedule.examine.exception.*;
import com.booking_schedule.examine.mapper.UserMapper;
import com.booking_schedule.examine.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private static final ObjectMapper AUDIT_JSON = new ObjectMapper();
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserAdminAuditRepository audits;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Transactional
    public UserResponse createAdminUser(AdminCreateUserRequest request) {
        if (request.getRoles() == null || request.getRoles().isEmpty()) throw new AppException(ErrorCode.INVALID_ROLE);
        return create(request, request.getRoles());
    }

    /** Legacy provisioning supports roleless accounts pending profile creation; requested roles remain atomic. */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        return create(request, request instanceof AdminCreateUserRequest admin ? admin.getRoles() : null);
    }

    private UserResponse create(CreateUserRequest request, Set<String> requestedRoles) {
        User actor = beginAdminWrite();
        validatePassword(request.getPassword());
        Set<Role> roles = requestedRoles == null ? new HashSet<>() : resolveRoles(actor, null, requestedRoles);
        String username = UserIdentitySupport.username(request.getUsername());
        String email = UserIdentitySupport.email(request.getEmail());
        UserIdentitySupport.checkAvailable(userRepository, null, username, email);
        User user = User.builder().username(username).email(email).fullName(UserIdentitySupport.text(request.getFullName()))
                .phone(UserIdentitySupport.text(request.getPhone())).password(passwordEncoder.encode(request.getPassword()))
                .roles(roles).status("ACTIVE").build();
        persist(user);
        audit(actor, user, "CREATE", null, true);
        return response(user, actor, activeSuperAdmins());
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) { return update(userId, request, "UPDATE"); }

    private UserResponse update(Long userId, UpdateUserRequest request, String action) {
        User actor = beginAdminWrite();
        User user = locked(userId);
        protectSuperAdmin(actor, user);
        var beforeState = snapshot(user);
        Set<ERole> beforeRoles = roleNames(user);
        Set<Role> desiredRoles = request.getRoles() == null ? user.getRoles() : resolveRoles(actor, userId, request.getRoles());
        String status = request.getStatus() == null ? user.getStatus() : request.getStatus();
        guardMutation(actor, user, status, roleNames(desiredRoles));
        if (request.getPassword() != null && actor.getId().equals(userId))
            throw new AppException(ErrorCode.USER_SELF_PASSWORD_RESET_NOT_ALLOWED);
        String email = request.getEmail() == null ? user.getEmail() : UserIdentitySupport.email(request.getEmail());
        UserIdentitySupport.checkAvailable(userRepository, userId, null, email);
        String oldStatus = user.getStatus();
        if (request.getFullName() != null) user.setFullName(UserIdentitySupport.text(request.getFullName()));
        if (request.getEmail() != null) user.setEmail(email);
        if (request.getPhone() != null) user.setPhone(UserIdentitySupport.text(request.getPhone()));
        user.setRoles(new HashSet<>(desiredRoles));
        user.setStatus(status);
        if (request.getPassword() != null) {
            validatePassword(request.getPassword());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getPassword() != null || !Objects.equals(oldStatus, status) || !beforeRoles.equals(roleNames(user)))
            user.revokeTokens();
        persist(user);
        audit(actor, user, action, beforeState, request.getPassword() != null);
        return response(user, actor, activeSuperAdmins());
    }

    @Transactional
    public UserResponse updateStatus(Long id, String status) {
        if (status == null) throw new AppException(ErrorCode.USER_INVALID_REQUEST);
        return update(id, UpdateUserRequest.builder().status(status).build(), "STATUS_CHANGE");
    }

    @Transactional
    public UserResponse updateRoles(Long id, Set<String> roles) {
        if (roles == null) throw new AppException(ErrorCode.INVALID_ROLE);
        return update(id, UpdateUserRequest.builder().roles(roles).build(), "ROLES_CHANGE");
    }

    @Transactional
    public UserResponse resetPassword(Long id, String newPassword) {
        validatePassword(newPassword);
        return update(id, UpdateUserRequest.builder().password(newPassword).build(), "PASSWORD_RESET");
    }

    @Transactional
    public UserDeleteResponse deleteUser(Long userId) {
        User actor = beginAdminWrite();
        User user = locked(userId);
        protectSuperAdmin(actor, user);
        guardMutation(actor, user, "INACTIVE", roleNames(user));
        var before = snapshot(user);
        user.setStatus("INACTIVE");
        user.revokeTokens();
        persist(user);
        audit(actor, user, "DEACTIVATE", before, false);
        return new UserDeleteResponse(userId, "DEACTIVATED", "INACTIVE", "User account has been deactivated; history is retained");
    }

    @Transactional
    public UserResponse updateMyInfo(UpdateMyUserRequest request) {
        User user = locked(currentUser().getId());
        if (!"ACTIVE".equals(user.getStatus())) throw new AppException(ErrorCode.USER_INACTIVE);
        var before = snapshot(user);
        if (request.getPassword() != null) verifyCurrentPassword(user, request.getCurrentPassword(), request.getPassword());
        String email = request.getEmail() == null ? user.getEmail() : UserIdentitySupport.email(request.getEmail());
        UserIdentitySupport.checkAvailable(userRepository, user.getId(), null, email);
        if (request.getFullName() != null) user.setFullName(UserIdentitySupport.text(request.getFullName()));
        if (request.getEmail() != null) user.setEmail(email);
        if (request.getPhone() != null) user.setPhone(UserIdentitySupport.text(request.getPhone()));
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.revokeTokens();
        }
        persist(user);
        if (request.getPassword() != null) audit(user, user, "SELF_PASSWORD_CHANGE", before, true);
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse changeMyPassword(ChangeMyPasswordRequest request) {
        var update = new UpdateMyUserRequest();
        update.setCurrentPassword(request.getCurrentPassword());
        update.setPassword(request.getNewPassword());
        return updateMyInfo(update);
    }

    private void verifyCurrentPassword(User user, String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isBlank()) throw new AppException(ErrorCode.USER_CURRENT_PASSWORD_REQUIRED);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) throw new AppException(ErrorCode.USER_CURRENT_PASSWORD_INVALID);
        validatePassword(newPassword);
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank() || password.length() < 6 || password.length() > 100)
            throw new AppException(ErrorCode.INVALID_PASSWORD);
    }

    public UserResponse getMyInfo() { return userMapper.toUserResponse(currentUser()); }

    public List<UserResponse> getUsers() {
        User actor = requireAdmin();
        long count = activeSuperAdmins();
        return userRepository.findAll().stream().map(user -> response(user, actor, count)).toList();
    }

    public UserResponse getUser(Long id) {
        User actor = requireAdmin();
        return response(userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)), actor, activeSuperAdmins());
    }

    public Page<UserResponse> search(UserSearchRequest request, Pageable pageable) {
        User actor = requireAdmin();
        if (request.getStatus() != null && !Set.of("ACTIVE", "INACTIVE").contains(request.getStatus()))
            throw new AppException(ErrorCode.USER_INVALID_REQUEST);
        Specification<User> spec = (root, query, cb) -> cb.conjunction();
        if (request.getStatus() != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), request.getStatus()));
        if (request.getRole() != null) spec = spec.and((r, q, cb) -> cb.equal(r.join("roles").get("name"), request.getRole()));
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String pattern = "%" + request.getKeyword().trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((r, q, cb) -> cb.or(cb.like(cb.lower(r.get("username")), pattern, '\\'),
                    cb.like(cb.lower(r.get("fullName")), pattern, '\\'), cb.like(cb.lower(r.get("email")), pattern, '\\'),
                    cb.like(cb.lower(r.get("phone")), pattern, '\\')));
        }
        long count = activeSuperAdmins();
        return userRepository.findAll(spec, pageable).map(user -> response(user, actor, count));
    }

    public Page<UserAuditResponse> auditLogs(Long id, Pageable pageable) {
        requireAdmin();
        if (!userRepository.existsById(id)) throw new AppException(ErrorCode.USER_NOT_EXISTED);
        return audits.findByTargetUserId(id, pageable).map(row -> new UserAuditResponse(row.getId(), row.getActorUserId(),
                row.getTargetUserId(), row.getAction(), row.isPasswordChanged(), row.getBeforeState(), row.getAfterState(), row.getCreatedAt()));
    }

    private UserResponse response(User user, User actor, long superAdmins) {
        UserResponse result = userMapper.toUserResponse(user);
        result.setCapabilities(profileCapabilities(user, actor, superAdmins));
        return result;
    }

    UserResponse.Capabilities profileCapabilities(User user, User actor, long superAdmins) {
        boolean self = user.getId().equals(actor.getId());
        boolean last = "ACTIVE".equals(user.getStatus()) && roleNames(user).contains(ERole.SUPER_ADMIN) && superAdmins <= 1;
        boolean editable = !roleNames(user).contains(ERole.SUPER_ADMIN) || roleNames(actor).contains(ERole.SUPER_ADMIN);
        return new UserResponse.Capabilities(self, last, editable, editable && !self && !last,
                editable, editable && !self, editable && !self && !last);
    }

    record ProfileReadContext(User actor, long activeSuperAdmins) {}
    ProfileReadContext profileReadContext() { return new ProfileReadContext(requireAdmin(), activeSuperAdmins()); }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    User beginProfileAdminWrite() { return beginAdminWrite(); }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    User lockProfileTarget(User actor, Long userId) {
        User target = locked(userId);
        protectSuperAdmin(actor, target);
        return target;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    void linkPatientRole(User actor, User target) {
        if (!patientRepository.existsByUserId(target.getId())) throw new AppException(ErrorCode.ROLE_PROFILE_CONFLICT);
        var before = snapshot(target);
        target.getRoles().add(roleRepository.findByName(ERole.PATIENT).orElseThrow(() -> new AppException(ErrorCode.INVALID_ROLE)));
        target.revokeTokens();
        persist(target);
        audit(actor, target, "PATIENT_PROFILE_LINKED", before, false);
    }

    private Set<Role> resolveRoles(User actor, Long targetId, Set<String> requested) {
        if (requested.isEmpty()) throw new AppException(ErrorCode.INVALID_ROLE);
        Set<ERole> desired = new HashSet<>();
        try { requested.forEach(name -> desired.add(ERole.valueOf(name))); }
        catch (IllegalArgumentException | NullPointerException ex) { throw new AppException(ErrorCode.INVALID_ROLE); }
        if (desired.contains(ERole.SUPER_ADMIN) && !roleNames(actor).contains(ERole.SUPER_ADMIN))
            throw new AppException(ErrorCode.UNAUTHORIZED);
        boolean patient = targetId != null && patientRepository.existsByUserId(targetId);
        boolean doctor = targetId != null && doctorRepository.existsByUserId(targetId);
        if (patient != desired.contains(ERole.PATIENT) || doctor != desired.contains(ERole.DOCTOR))
            throw new AppException(ErrorCode.ROLE_PROFILE_CONFLICT);
        List<Role> roles = roleRepository.findByNameIn(desired);
        if (roles.size() != desired.size()) throw new AppException(ErrorCode.INVALID_ROLE);
        return new HashSet<>(roles);
    }

    private void guardMutation(User actor, User target, String status, Set<ERole> roles) {
        if (!Set.of("ACTIVE", "INACTIVE").contains(status)) throw new AppException(ErrorCode.USER_INVALID_REQUEST);
        boolean self = actor.getId().equals(target.getId());
        if (self && "INACTIVE".equals(status)) throw new AppException(ErrorCode.USER_SELF_DEACTIVATION_NOT_ALLOWED);
        if ("ACTIVE".equals(target.getStatus()) && roleNames(target).contains(ERole.SUPER_ADMIN)
                && (!"ACTIVE".equals(status) || !roles.contains(ERole.SUPER_ADMIN)) && activeSuperAdmins() <= 1)
            throw new AppException(ErrorCode.USER_LAST_SUPER_ADMIN);
        if (self && !roles.contains(ERole.ADMIN) && !roles.contains(ERole.SUPER_ADMIN))
            throw new AppException(ErrorCode.USER_SELF_ROLE_CHANGE_NOT_ALLOWED);
    }

    private long activeSuperAdmins() { return userRepository.countByStatusAndRolesName("ACTIVE", ERole.SUPER_ADMIN); }

    private User beginAdminWrite() {
        roleRepository.findLockedByName(ERole.SUPER_ADMIN).orElseThrow(() -> new AppException(ErrorCode.INVALID_ROLE));
        User actor = currentUser();
        entityManager.refresh(actor, LockModeType.PESSIMISTIC_WRITE);
        if (!"ACTIVE".equals(actor.getStatus())) throw new AppException(ErrorCode.USER_INACTIVE);
        if (!roleNames(actor).contains(ERole.ADMIN) && !roleNames(actor).contains(ERole.SUPER_ADMIN))
            throw new AppException(ErrorCode.UNAUTHORIZED);
        return actor;
    }

    private User locked(Long id) {
        User user = userRepository.findLockedById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        return user;
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AppException(ErrorCode.UNAUTHENTICATED);
        User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        if (!"ACTIVE".equals(user.getStatus())) throw new AppException(ErrorCode.USER_INACTIVE);
        return user;
    }

    private User requireAdmin() {
        User user = currentUser();
        if (!roleNames(user).contains(ERole.ADMIN) && !roleNames(user).contains(ERole.SUPER_ADMIN))
            throw new AppException(ErrorCode.UNAUTHORIZED);
        return user;
    }

    private void protectSuperAdmin(User actor, User target) {
        if (roleNames(target).contains(ERole.SUPER_ADMIN) && !roleNames(actor).contains(ERole.SUPER_ADMIN))
            throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    private Set<ERole> roleNames(User user) { return roleNames(user.getRoles()); }
    private Set<ERole> roleNames(Set<Role> roles) {
        Set<ERole> names = new HashSet<>();
        roles.forEach(role -> names.add(role.getName()));
        return names;
    }

    private User persist(User user) {
        try { return userRepository.saveAndFlush(user); }
        catch (DataIntegrityViolationException ex) { throw UserIdentitySupport.conflict(ex); }
    }

    private Map<String, Object> snapshot(User user) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("username", user.getUsername());
        state.put("fullName", user.getFullName());
        state.put("email", user.getEmail());
        state.put("phone", user.getPhone());
        state.put("status", user.getStatus());
        state.put("roles", roleNames(user).stream().map(Enum::name).sorted().toList());
        return state;
    }

    private void audit(User actor, User target, String action, Map<String, Object> before, boolean passwordChanged) {
        try {
            audits.save(UserAdminAudit.builder().actorUserId(actor.getId()).targetUserId(target.getId()).action(action)
                    .passwordChanged(passwordChanged).beforeState(before == null ? null : AUDIT_JSON.writeValueAsString(before))
                    .afterState(AUDIT_JSON.writeValueAsString(snapshot(target))).build());
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize audit metadata", ex); }
    }
}
