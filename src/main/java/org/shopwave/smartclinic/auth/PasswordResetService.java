package org.shopwave.smartclinic.auth;

import org.shopwave.smartclinic.admin.AdminService;
import org.shopwave.smartclinic.doctor.DoctorService;
import org.shopwave.smartclinic.email.EmailService;
import org.shopwave.smartclinic.patient.PatientService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages password reset for self-service (patient) and admin-triggered
 * (doctor) flows.
 * <p>
 * For MVP, reset tokens are stored in memory. In production, persist tokens
 * (database/Redis) with expiration and single-use enforcement.
 */
@Service
public class PasswordResetService {

    private final Map<String, Long> patientResetTokens = new HashMap<>();
    private final Map<String, String> doctorResetTokens = new HashMap<>();
    private final EmailService emailService;
    private final PatientService patientService;
    private final AdminService adminService;
    private final DoctorService doctorService;

    public PasswordResetService(EmailService emailService,
                                PatientService patientService,
                                AdminService adminService,
                                DoctorService doctorService) {
        this.emailService = emailService;
        this.patientService = patientService;
        this.adminService = adminService;
        this.doctorService = doctorService;
    }

    /**
     * Patient requests a password reset. A token is generated and emailed to
     * the patient's registered email with a reset link.
     */
    public String requestPatientReset(String email) {
        Long patientId = patientService.findIdByEmail(email);
        if (patientId == null) {
            throw new IllegalArgumentException("No patient found with email: " + email);
        }

        String token = UUID.randomUUID().toString();
        patientResetTokens.put(token, patientId);
        emailService.sendPasswordResetLink(email, "Patient",
                "http://localhost:8080/reset-password?token=" + token);
        return token;
    }

    /**
     * Admin triggers a password reset for a doctor. A temporary password is
     * generated, set on the doctor's account, and emailed to the doctor.
     */
    public void triggerDoctorReset(Long doctorId, String doctorEmail) {
        String tempPassword = UUID.randomUUID().toString().substring(0, 12);

        String token = UUID.randomUUID().toString();
        doctorResetTokens.put(token, doctorId + ":" + tempPassword);

        emailService.sendTemporaryPassword(doctorEmail, "Doctor", tempPassword);
    }

    /**
     * Patient completes password reset using a valid token.
     */
    public void completePatientReset(String token, String newPassword) {
        Long patientId = patientResetTokens.get(token);
        if (patientId == null) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        patientService.forceResetPassword(patientId, newPassword);
        patientResetTokens.remove(token);
    }

    /**
     * Doctor completes admin-triggered reset using token and temp password.
     */
    public void completeDoctorReset(String token, String tempPassword, String newPassword) {
        String stored = doctorResetTokens.get(token);
        if (stored == null) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        String[] parts = stored.split(":");
        Long doctorId = Long.valueOf(parts[0]);
        String expectedTemp = parts[1];

        if (!expectedTemp.equals(tempPassword)) {
            throw new IllegalArgumentException("Temporary password is incorrect");
        }

        doctorService.changePassword(doctorId, tempPassword, newPassword);
        doctorResetTokens.remove(token);
    }
}
