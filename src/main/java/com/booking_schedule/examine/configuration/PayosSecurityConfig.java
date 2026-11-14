package com.booking_schedule.examine.configuration;

import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class PayosSecurityConfig {
    // No patient JWT on webhook. Every request is verified with the channel checksum key in PayosGateway.
    @Bean @Order(2)
    SecurityFilterChain payosWebhookChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/payments/payos/webhook")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll()).build();
    }
}
