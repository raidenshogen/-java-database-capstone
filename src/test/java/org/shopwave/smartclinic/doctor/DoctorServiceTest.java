package org.shopwave.smartclinic.doctor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shopwave.smartclinic.dto.DoctorRequest;
import org.shopwave.smartclinic.model.Doctor;
import org.shopwave.smartclinic.repository.DoctorRepository;
import org.shopwave.smartclinic.repository.SpecialtyRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DoctorService} — password change and email uniqueness.
 */
@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private SpecialtyRepository specialtyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DoctorService doctorService;

    @Test
    void changePasswordShouldRejectWrongCurrentPassword() {
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setPasswordHash("old-hash");

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> doctorService.changePassword(1L, "wrong", "new-pass"));
        assertTrue(ex.getMessage().contains("Current password is incorrect"));
    }

    @Test
    void changePasswordShouldSucceedWithCorrectCurrentPassword() {
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setPasswordHash("old-hash");

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");

        doctorService.changePassword(1L, "current", "new-pass");

        verify(doctorRepository).save(doctor);
    }

    @Test
    void createShouldRejectDuplicateEmail() {
        DoctorRequest request = DoctorRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password")
                .specialtyId(1L)
                .build();

        when(doctorRepository.existsByEmail("john@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> doctorService.create(request));
        assertTrue(ex.getMessage().contains("already registered"));
    }
}
