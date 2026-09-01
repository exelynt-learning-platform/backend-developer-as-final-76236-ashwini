package com.example.booking.config;

import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.enums.Role;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds a baseline ADMIN and USER account plus a handful of sample
 * resources on application startup. Idempotent: re-running the
 * application will not create duplicates.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedResources();
    }

    private void seedUsers() {
        if (!userRepository.existsByEmail("admin@example.com")) {
            userRepository.save(User.builder()
                    .name("System Administrator")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .build());
        }

        if (!userRepository.existsByEmail("user@example.com")) {
            userRepository.save(User.builder()
                    .name("Standard User")
                    .email("user@example.com")
                    .password(passwordEncoder.encode("User@123"))
                    .role(Role.USER)
                    .build());
        }
    }

    private void seedResources() {
        seedResourceIfMissing("Conference Room A", "Large conference room with projector and video conferencing", new BigDecimal("50.00"));
        seedResourceIfMissing("Company Car", "Sedan available for business trips", new BigDecimal("100.00"));
        seedResourceIfMissing("Projector", "Portable HD projector", new BigDecimal("20.00"));
    }

    private void seedResourceIfMissing(String name, String description, BigDecimal price) {
        if (!resourceRepository.existsByNameIgnoreCase(name)) {
            resourceRepository.save(Resource.builder()
                    .name(name)
                    .description(description)
                    .price(price)
                    .available(true)
                    .build());
        }
    }
}
