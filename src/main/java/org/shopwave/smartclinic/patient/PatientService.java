package org.shopwave.smartclinic.patient;

import org.shopwave.smartclinic.dto.PatientRequest;
import org.shopwave.smartclinic.dto.PatientResponse;
import org.shopwave.smartclinic.model.Patient;
import org.shopwave.smartclinic.repository.PatientRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for {@link Patient} accounts (Admin-side CRUD).
 * <p>
 * Patients are created by Admin only — there is no public self-registration
 * in MVP (see {@code user_stories.md}). Passwords are BCrypt hashed.
 */
@Service
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    public PatientService(PatientRepository patientRepository,
                          PasswordEncoder passwordEncoder) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<PatientResponse> findAllActive() {
        return patientRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PatientResponse findById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + id));
        return toResponse(patient);
    }

    @Transactional
    public PatientResponse create(PatientRequest request) {
        if (patientRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: "
                    + request.getEmail());
        }

        Patient patient = Patient.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .assignedDoctorId(request.getAssignedDoctorId())
                .isActive(true)
                .build();
        return toResponse(patientRepository.save(patient));
    }

    @Transactional
    public PatientResponse update(Long id, PatientRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + id));

        // Email uniqueness check (excluding self)
        patientRepository.findByEmail(request.getEmail()).ifPresent(p -> {
            if (!p.getId().equals(id)) {
                throw new IllegalArgumentException("Email already registered: "
                        + request.getEmail());
            }
        });

        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setEmail(request.getEmail());
        patient.setPhone(request.getPhone());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setAssignedDoctorId(request.getAssignedDoctorId());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            patient.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return toResponse(patientRepository.save(patient));
    }

    @Transactional
    public void deactivate(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + id));
        patient.setIsActive(false);
        patientRepository.save(patient);
    }

    @Transactional
    public void activate(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + id));
        patient.setIsActive(true);
        patientRepository.save(patient);
    }

    @Transactional
    public void changePassword(Long id, String currentPassword, String newPassword) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + id));
        if (!passwordEncoder.matches(currentPassword, patient.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        patient.setPasswordHash(passwordEncoder.encode(newPassword));
        patientRepository.save(patient);
    }

    public Long findIdByEmail(String email) {
        return patientRepository.findByEmail(email)
                .map(Patient::getId)
                .orElse(null);
    }

    @Transactional
    public void forceResetPassword(Long id, String newPassword) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + id));
        patient.setPasswordHash(passwordEncoder.encode(newPassword));
        patientRepository.save(patient);
    }

    private PatientResponse toResponse(Patient patient) {
        return PatientResponse.builder()
                .id(patient.getId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .email(patient.getEmail())
                .phone(patient.getPhone())
                .dateOfBirth(patient.getDateOfBirth())
                .assignedDoctorId(patient.getAssignedDoctorId())
                .active(patient.getIsActive() != null && patient.getIsActive())
                .build();
    }
}
