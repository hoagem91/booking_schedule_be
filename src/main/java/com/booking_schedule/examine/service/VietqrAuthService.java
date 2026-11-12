package com.booking_schedule.examine.service;
import com.booking_schedule.examine.configuration.VietqrProperties;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
@Service @RequiredArgsConstructor
public class VietqrAuthService {
    private final VietqrProperties properties;
    public void requireEnabled() {
        if (!properties.isEnabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "VietQR disabled");
    }
    public Map<String,Object> issue(String authorization) {
        requireEnabled();
        try {
            if (properties.getTokenUsername().isBlank() || properties.getTokenPassword().isBlank()
                    || properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8).length < 64)
                throw new IllegalStateException();
            if (authorization == null || !authorization.startsWith("Basic ")) throw new IllegalArgumentException();
            byte[] received = Base64.getDecoder().decode(authorization.substring(6));
            byte[] expected = (properties.getTokenUsername()+":"+properties.getTokenPassword()).getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(expected, received)) throw new IllegalArgumentException();
            Instant now = Instant.now();
            int ttl = Math.max(1, Math.min(300, properties.getWebhookTokenTtlSeconds()));
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), new JWTClaimsSet.Builder()
                    .issuer("clinic-vietqr").audience("vietqr-callback").subject("vietqr")
                    .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(ttl))).build());
            jwt.sign(new MACSigner(properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8)));
            return Map.of("access_token",jwt.serialize(),"token_type","Bearer","expires_in",ttl);
        } catch (Exception exception) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid callback credentials"); }
    }
    public void verify(String authorization) {
        requireEnabled();
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) throw new IllegalArgumentException();
            SignedJWT jwt = SignedJWT.parse(authorization.substring(7));
            var claims = jwt.getJWTClaimsSet();
            if (!JWSAlgorithm.HS512.equals(jwt.getHeader().getAlgorithm())
                    || !jwt.verify(new MACVerifier(properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8)))
                    || !"clinic-vietqr".equals(claims.getIssuer()) || !claims.getAudience().equals(List.of("vietqr-callback"))
                    || !"vietqr".equals(claims.getSubject()) || claims.getExpirationTime() == null
                    || !claims.getExpirationTime().toInstant().isAfter(Instant.now())) throw new IllegalArgumentException();
        } catch (Exception exception) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid callback token"); }
    }
}
