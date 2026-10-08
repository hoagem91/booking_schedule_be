package com.booking_schedule.examine.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    private static final String[] PUBLIC_ENDPOINTS = {
           "/users/{userId}", "/users/registration", "/auth/token", "/auth/introspect", "/auth/logout", "/auth/refresh"
    };

    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/users/{userId}"
    };

    private final CustomJwtDecoder customJwtDecoder;

    public SecurityConfig(CustomJwtDecoder customJwtDecoder) {
        this.customJwtDecoder = customJwtDecoder;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(request -> request
                .requestMatchers("/api/admin/time-slots", "/api/admin/time-slots/**")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN", "SCOPE_STAFF")
                .requestMatchers(HttpMethod.GET, "/api/public/time-slots", "/api/public/time-slots/**",
                        "/api/public/doctor-schedules/*/time-slots").permitAll()
                .requestMatchers("/api/admin/doctor-schedules", "/api/admin/doctor-schedules/**")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/public/doctor-schedules").permitAll()
                .requestMatchers("/api/admin/services", "/api/admin/services/**")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/public/services", "/api/public/services/**").permitAll()
                .requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                .anyRequest()
                .authenticated());

        httpSecurity.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwtConfigurer -> jwtConfigurer
                        .decoder(customJwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint()));
        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }
}
