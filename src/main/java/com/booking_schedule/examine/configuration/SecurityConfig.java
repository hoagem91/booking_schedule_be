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
           "/users/registration", "/auth/register", "/auth/token", "/auth/introspect", "/auth/logout", "/auth/refresh"
    };

    private final CustomJwtDecoder customJwtDecoder;

    public SecurityConfig(CustomJwtDecoder customJwtDecoder) {
        this.customJwtDecoder = customJwtDecoder;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(request -> request
                .requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers("/users/me", "/users/me/password").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/public/specialties").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/admin/specialties", "/api/admin/specialties/**",
                        "/admin/specialties", "/admin/specialties/**")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN", "SCOPE_STAFF")
                .requestMatchers(HttpMethod.POST, "/api/admin/specialties/search", "/admin/specialties/search")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN", "SCOPE_STAFF")
                .requestMatchers("/api/admin/specialties", "/api/admin/specialties/**",
                        "/admin/specialties", "/admin/specialties/**")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/public/rooms", "/api/public/specialties/*/rooms").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/admin/rooms/search", "/api/admin/rooms/availability/search")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN", "SCOPE_STAFF")
                .requestMatchers(HttpMethod.GET, "/api/admin/rooms", "/api/admin/rooms/**")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN", "SCOPE_STAFF")
                .requestMatchers("/api/admin/rooms", "/api/admin/rooms/**")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN")
                .requestMatchers("/users", "/users/**", "/api/admin/users", "/api/admin/users/**", "/api/admin/roles",
                        "/api/admin/doctors", "/api/admin/doctors/**")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST, "/patients").hasAnyAuthority("SCOPE_PATIENT", "SCOPE_DOCTOR")
                .requestMatchers("/patients/me").hasAuthority("SCOPE_PATIENT")
                .requestMatchers("/patients", "/patients/**", "/api/admin/patients", "/api/admin/patients/**")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN")
                .requestMatchers("/api/admin/appointments", "/api/admin/appointments/**")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN", "SCOPE_STAFF")
                .requestMatchers("/api/patient/appointments", "/api/patient/appointments/**")
                .hasAuthority("SCOPE_PATIENT")
                .requestMatchers("/api/admin/time-slots", "/api/admin/time-slots/**")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN", "SCOPE_STAFF")
                .requestMatchers(HttpMethod.GET, "/api/public/time-slots", "/api/public/time-slots/**",
                        "/api/public/doctor-schedules/*/time-slots").permitAll()
                .requestMatchers("/api/admin/doctor-schedules", "/api/admin/doctor-schedules/**")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/public/doctor-schedules/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/admin/services", "/api/admin/services/**",
                        "/admin/services", "/admin/services/**")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN", "SCOPE_STAFF")
                .requestMatchers(HttpMethod.POST, "/api/admin/services/search", "/admin/services/search")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN", "SCOPE_STAFF")
                .requestMatchers("/api/admin/services", "/api/admin/services/**", "/admin/services", "/admin/services/**")
                .hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/public/services", "/api/public/services/**").permitAll()
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
