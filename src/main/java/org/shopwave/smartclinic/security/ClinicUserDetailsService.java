package org.shopwave.smartclinic.security;

import org.shopwave.smartclinic.repository.AdminRepository;
import org.shopwave.smartclinic.repository.DoctorRepository;
import org.shopwave.smartclinic.repository.PatientRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Loads a user for authentication by checking all three account tables in order:
 * Admin, then Doctor, then Patient.
 * <p>
 * The login identifier (username/email) is unique across all three tables in
 * practice (admin username vs doctor/patient email), so a single lookup field
 * works for all roles.
 */
@Service
public class ClinicUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public ClinicUserDetailsService(AdminRepository adminRepository,
                                    DoctorRepository doctorRepository,
                                    PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        Objects.requireNonNull(username, "username must not be null");
        String lookup = username.trim();

        // 1. Admin (username-based)
        return adminRepository.findByUsername(lookup)
                .map(ClinicUserDetails::fromAdmin)
                .or(() -> doctorRepository.findByEmail(lookup)
                        .map(ClinicUserDetails::fromDoctor))
                .or(() -> patientRepository.findByEmail(lookup)
                        .map(ClinicUserDetails::fromPatient))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with username/email: " + lookup));
    }
}
