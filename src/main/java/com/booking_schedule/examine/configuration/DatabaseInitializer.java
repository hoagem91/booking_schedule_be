package com.booking_schedule.examine.configuration;

import com.booking_schedule.examine.entity.Role;
import com.booking_schedule.examine.enums.ERole;
import com.booking_schedule.examine.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final com.booking_schedule.examine.repository.UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing database...");
        
        // Initialize Roles if empty
        Arrays.stream(ERole.values()).forEach(eRole -> {
            if (roleRepository.findByName(eRole).isEmpty()) {
                Role role = Role.builder()
                        .name(eRole)
                        .build();
                roleRepository.save(role);
                log.info("Initialized role: {}", eRole.name());
            }
        });

        // Initialize admin user if empty
        if (!userRepository.existsByUsername("admin")) {
            com.booking_schedule.examine.entity.User adminUser = com.booking_schedule.examine.entity.User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin"))
                    .fullName("Administrator")
                    .email("admin@booking-schedule.com")
                    .status("ACTIVE")
                    .build();

            roleRepository.findByName(ERole.ADMIN).ifPresent(role -> {
                adminUser.getRoles().add(role);
            });
            roleRepository.findByName(ERole.SUPER_ADMIN).ifPresent(role -> {
                adminUser.getRoles().add(role);
            });

            userRepository.save(adminUser);
            log.info("Initialized admin user.");
        }
        
        log.info("Database initialization completed.");
    }
}
