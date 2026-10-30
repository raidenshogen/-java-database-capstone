package org.shopwave.smartclinic.patient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shopwave.smartclinic.dto.PatientRequest;
import org.shopwave.smartclinic.model.Patient;
import org.shopwave.smartclinic.repository.PatientRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PatientService} — email uniqueness and password change.
 */
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PatientService patientService;

    @Test
    void createShouldRejectDuplicateEmail() {
        PatientRequest request = PatientRequest.builder()
                .firstName("Maria")
                .lastName("Gomez")
                .email("maria@example.com")
                .password("password")
                .phone("+1-555-0147")
                .build();

        when(patientRepository.existsByEmail("maria@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> patientService.create(request));
        assertTrue(ex.getMessage().contains("already registered"));
    }

    @Test
    void changePasswordShouldRejectWrongCurrentPassword() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setPasswordHash("old-hash");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> patientService.changePassword(1L, "wrong", "new-pass"));
        assertTrue(ex.getMessage().contains("Current password is incorrect"));
    }

    @Test
    void changePasswordShouldSucceedWithCorrectCurrentPassword() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setPasswordHash("old-hash");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");

        patientService.changePassword(1L, "current", "new-pass");

        verify(patientRepository).save(patient);
    }
}
