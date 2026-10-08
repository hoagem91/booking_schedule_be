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
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean passwordMatched = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!passwordMatched) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String token = generateToken(user);
        Date expiryTime = new Date(Instant.now().plus(validDuration, ChronoUnit.SECONDS).toEpochMilli());

        return AuthenticationResponse.builder()
                .token(token)
                .expiryTime(expiryTime)
                .authenticated(true)
                .build();
    }

    // ======================== INTROSPECT ========================
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
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) throws ParseException, JOSEException {
        // 1. Verify old token (allow expired, but check blacklist & signature)
        SignedJWT signedJWT = verifyToken(request.getToken(), true);

        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        log.info("ExpiredTime: {}", expiryTime);

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
        String newToken = generateToken(user);
        Date newExpiry = new Date(Instant.now().plus(validDuration, ChronoUnit.SECONDS).toEpochMilli());
        log.info("ExpiredTimeNew: {}", newExpiry);

        return AuthenticationResponse.builder()
                .token(newToken)
                .expiryTime(newExpiry)
                .authenticated(true)
                .build();
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
        if (!allowExpired && expiryTime.before(new Date())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Check blacklist
        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        if (invalidatedTokenRepository.existsById(jwtId)) {
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
