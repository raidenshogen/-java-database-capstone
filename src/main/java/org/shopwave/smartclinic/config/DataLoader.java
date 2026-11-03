package org.shopwave.smartclinic.config;

import org.shopwave.smartclinic.model.Admin;
import org.shopwave.smartclinic.model.Specialty;
import org.shopwave.smartclinic.repository.AdminRepository;
import org.shopwave.smartclinic.repository.SpecialtyRepository;
import org.shopwave.smartclinic.security.ClinicUserDetailsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

import java.time.LocalDateTime;

/**
 * Seeds the database on startup.
 * <p>
 * Creates:
 * <ul>
 *   <li>The single Admin account is created from environment-backed properties.
 *       Only one admin is ever expected.</li>
 *   <li>Default clinic specialties used by the booking engine.</li>
 * </ul>
 * Idempotent — safe to run on every startup.
 */
@Configuration
public class DataLoader {

    @Bean
    public CommandLineRunner loadData(AdminRepository adminRepository,
                                      SpecialtyRepository specialtyRepository,
                                      PasswordEncoder passwordEncoder,
                                      @Value("${admin.username}") String adminUsername,
                                      @Value("${admin.password}") String adminPassword) {
        return args -> {
            // --- Seed admin (only one) ---
            if (adminRepository.count() == 0) {
                Admin admin = Admin.builder()
                        .username(adminUsername)
                        .email("admin@clinic.local")
                        .passwordHash(passwordEncoder.encode(adminPassword))
                        .createdAt(LocalDateTime.now())
                        .build();
                adminRepository.save(admin);
            }

            // --- Seed specialties ---
            List<String> defaults = List.of(
                    "Cardiology", "Dermatology", "Pediatrics",
                    "Orthopedics", "Neurology", "General Medicine"
            );
            for (String name : defaults) {
                if (!specialtyRepository.existsByName(name)) {
                    specialtyRepository.save(Specialty.builder()
                            .name(name)
                            .createdAt(LocalDateTime.now())
                            .build());
                }
            }
        };
    }
}
