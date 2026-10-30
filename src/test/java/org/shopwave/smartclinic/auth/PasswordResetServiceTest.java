package org.shopwave.smartclinic.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shopwave.smartclinic.doctor.DoctorService;
import org.shopwave.smartclinic.email.EmailService;
import org.shopwave.smartclinic.patient.PatientService;
import org.shopwave.smartclinic.admin.AdminService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PasswordResetService} — token-based reset flow.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private PatientService patientService;

    @Mock
    private DoctorService doctorService;

    @Mock
    private AdminService adminService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void requestPatientResetShouldRejectUnknownEmail() {
        when(patientService.findIdByEmail("unknown@example.com")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> passwordResetService.requestPatientReset("unknown@example.com"));
        assertTrue(ex.getMessage().contains("No patient found"));
    }

    @Test
    void completePatientResetShouldRejectInvalidToken() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> passwordResetService.completePatientReset("invalid-token", "new-pass"));
        assertTrue(ex.getMessage().contains("Invalid or expired"));
    }
}
