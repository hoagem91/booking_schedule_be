package com.booking_schedule.examine.service;

import com.booking_schedule.examine.dto.request.AuthenticationRequest;
import com.booking_schedule.examine.dto.request.IntrospectRequest;
import com.booking_schedule.examine.dto.request.LogoutRequest;
import com.booking_schedule.examine.dto.request.RefreshTokenRequest;
import com.booking_schedule.examine.dto.response.AuthenticationResponse;
import com.booking_schedule.examine.dto.response.IntrospectResponse;
import com.booking_schedule.examine.entity.InvalidatedToken;
import com.booking_schedule.examine.entity.User;
import com.booking_schedule.examine.exception.AppException;
import com.booking_schedule.examine.exception.ErrorCode;
import com.booking_schedule.examine.repository.InvalidatedTokenRepository;
import com.booking_schedule.examine.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationService {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    InvalidatedTokenRepository invalidatedTokenRepository;
    com.booking_schedule.examine.repository.PatientRepository patientRepository;
    com.booking_schedule.examine.repository.RoleRepository roleRepository;

    @org.springframework.transaction.annotation.Transactional
    public AuthenticationResponse registerPatient(com.booking_schedule.examine.dto.request.RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()) || userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        var role = roleRepository.findByName(com.booking_schedule.examine.enums.ERole.PATIENT)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ROLE));
        User user = User.builder().username(request.getUsername()).password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName()).email(request.getEmail()).phone(request.getPhone())
                .roles(new java.util.HashSet<>(java.util.Set.of(role))).build();
        try {
            userRepository.saveAndFlush(user);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        var patient = com.booking_schedule.examine.entity.Patient.builder().user(user)
                .patientCode(ProfileCodeGenerator.generate("BN")).dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender()).address(request.getAddress())
                .healthInsuranceCode(request.getHealthInsuranceCode()).build();
        patientRepository.saveAndFlush(patient);
        user.setPatient(patient);
        return authenticationResponse(user);
    }

    private AuthenticationResponse authenticationResponse(User user) {
        return AuthenticationResponse.builder().token(generateToken(user))
                .expiryTime(new Date(Instant.now().plus(validDuration, ChronoUnit.SECONDS).toEpochMilli()))
                .authenticated(true).userId(user.getId())
                .patientId(user.getPatient() == null ? null : user.getPatient().getId()).build();
    }

    @NonFinal
    @Value("${jwt.signerKey}")
    String signerKey;

    @NonFinal
    @Value("${jwt.valid-duration}")
    long validDuration;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    long refreshableDuration;

    // ======================== LOGIN ========================
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean passwordMatched = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!passwordMatched) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        if (!"ACTIVE".equals(user.getStatus())) throw new AppException(ErrorCode.USER_INACTIVE);
        return authenticationResponse(user);
    }

    // ======================== INTROSPECT ========================
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public IntrospectResponse introspect(IntrospectRequest request) {
        String token = request.getToken();
        boolean valid = true;

        try {
            verifyToken(token, false);
        } catch (AppException | JOSEException | ParseException e) {
            valid = false;
        }

        return IntrospectResponse.builder().valid(valid).build();
    }

    // ======================== LOGOUT ========================
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            SignedJWT signedJWT = verifyToken(request.getToken(), true);

            String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jwtId)
                    .expiryTime(expiryTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);
            log.info("Token {} has been invalidated (logout)", jwtId);

        } catch (AppException e) {
            log.info("Token already expired or invalid — logout skipped.");
        }
    }

    // ======================== REFRESH TOKEN ========================
    @org.springframework.transaction.annotation.Transactional
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) throws ParseException, JOSEException {
        // 1. Verify old token (allow expired, but check blacklist & signature)
        SignedJWT signedJWT = verifyToken(request.getToken(), true);

        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        // 2. Invalidate old token
        invalidatedTokenRepository.save(
                InvalidatedToken.builder()
                        .id(jwtId)
                        .expiryTime(expiryTime)
                        .build()
        );

        // 3. Get fresh user info from DB
        String username = signedJWT.getJWTClaimsSet().getSubject();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 4. Issue new token
        return authenticationResponse(user);
    }

    // ======================== INTERNAL: Generate JWT ========================
    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("booking-schedule.com")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(validDuration, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .claim("userId", user.getId())
                .claim("patientId", user.getPatient() == null ? null : user.getPatient().getId())
                .claim("authVersion", user.getAuthVersion())
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(signerKey.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    // ======================== INTERNAL: Verify JWT ========================
    /**
     * @param allowExpired nếu true → bỏ qua lỗi hết hạn (dùng khi logout/refresh)
     */
    private SignedJWT verifyToken(String token, boolean allowExpired) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(signerKey.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        // Verify signature
        if (!signedJWT.verify(verifier)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Check expiry (unless allowExpired)
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        Date issuedAt = signedJWT.getJWTClaimsSet().getIssueTime();
        if (expiryTime == null || issuedAt == null
                || (!allowExpired && !expiryTime.after(new Date()))
                || (allowExpired && !issuedAt.toInstant().plus(refreshableDuration, ChronoUnit.SECONDS).isAfter(Instant.now()))) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Check blacklist
        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        if (invalidatedTokenRepository.existsById(jwtId)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        User user = userRepository.findByUsername(signedJWT.getJWTClaimsSet().getSubject())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        if (!"ACTIVE".equals(user.getStatus())
                || !java.util.Objects.equals(user.getId(), signedJWT.getJWTClaimsSet().getLongClaim("userId"))
                || !java.util.Objects.equals(user.getAuthVersion(), signedJWT.getJWTClaimsSet().getLongClaim("authVersion"))) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return signedJWT;
    }

    // ======================== INTERNAL: Build Scope ========================
    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> stringJoiner.add("SCOPE_" + role.getName().name()));
        }
        return stringJoiner.toString();
    }
}
