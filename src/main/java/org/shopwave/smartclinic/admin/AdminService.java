package org.shopwave.smartclinic.admin;

import org.shopwave.smartclinic.model.Admin;
import org.shopwave.smartclinic.repository.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(AdminRepository adminRepository,
                        PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean exists() {
        return adminRepository.count() > 0;
    }

    /**
     * Creates the single admin account. Called once at setup; subsequent calls
     * throw to prevent duplicate admin creation.
     */
    @Transactional
    public Admin create(String username, String email, String rawPassword) {
        if (adminRepository.count() > 0) {
            throw new IllegalStateException("Admin account already exists");
        }
        if (adminRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken: " + username);
        }
        if (adminRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already taken: " + email);
        }

        Admin admin = Admin.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build();
        return adminRepository.save(admin);
    }

    @Transactional
    public void changePassword(String username, String newRawPassword) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Admin not found: " + username));
        admin.setPasswordHash(passwordEncoder.encode(newRawPassword));
        adminRepository.save(admin);
    }
}
