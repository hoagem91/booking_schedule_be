package com.booking_schedule.examine.configuration;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class VietqrSecurityConfig {
    // Callback credentials are independent of patient JWTs; verified inside VietqrAuthService.
    @Bean @Order(1)
    SecurityFilterChain vietqrCallbackChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/token_generate", "/bank/api/transaction-sync")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll()).build();
    }
}
