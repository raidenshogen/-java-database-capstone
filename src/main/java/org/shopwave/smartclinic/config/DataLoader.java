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

import java.util.List;

/**
 * Seeds the database on startup.
 * <p>
 * Creates:
 * <ul>
 *   <li>The single Admin account (from {@code admin.username} / {@code admin.password}
 *       properties, defaulting to admin/admin123). Only one admin is ever expected.</li>
 *   <li>Default clinic specialties used by the booking engine.</li>
 * </ul>
 * Idempotent — safe to run on every startup.
 */
@Configuration
public class DataLoader {

    @Bean
    public CommandLineRunner loadData(AdminRepository adminRepository,
                                      SpecialtyRepository specialtyRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            // --- Seed admin (only one) ---
            if (adminRepository.count() == 0) {
                Admin admin = Admin.builder()
                        .username("admin")
                        .email("admin@clinic.local")
                        .passwordHash(passwordEncoder.encode("admin123"))
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
                    specialtyRepository.save(Specialty.builder().name(name).build());
                }
            }
        };
    }
}
