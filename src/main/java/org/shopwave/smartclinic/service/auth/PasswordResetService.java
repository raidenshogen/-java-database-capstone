package org.shopwave.smartclinic.service.auth;

import org.shopwave.smartclinic.service.admin.AdminService;
import org.shopwave.smartclinic.service.doctor.DoctorService;
import org.shopwave.smartclinic.service.email.EmailService;
import org.shopwave.smartclinic.service.patient.PatientService;
import org.springframework.stereotype.Service;
import org.springframework.mail.MailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Manages password reset for self-service (patient) and admin-triggered
 * (doctor) flows.
 * <p>
 * For MVP, reset tokens are stored in memory with expiration. In production,
 * persist tokens (database/Redis) with expiration and single-use enforcement.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    // Token validity: 1 hour in milliseconds
    private static final long TOKEN_VALIDITY_MS = 3600000;

    private final Map<String, ResetToken> patientResetTokens = new ConcurrentHashMap<>();
    private final Map<String, ResetToken> doctorResetTokens = new ConcurrentHashMap<>();
    private final EmailService emailService;
    private final PatientService patientService;
    private final AdminService adminService;
    private final DoctorService doctorService;
    private final String baseUrl;

    public PasswordResetService(EmailService emailService,
                                PatientService patientService,
                                AdminService adminService,
                                DoctorService doctorService,
                                @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.emailService = emailService;
        this.patientService = patientService;
        this.adminService = adminService;
        this.doctorService = doctorService;
        this.baseUrl = baseUrl;
    }

    /** Patient requests a password reset. A token is generated and returned.
     * The token is also emailed to the patient's registered email with a reset link
     * if SMTP is configured. If SMTP is not configured, the token is still valid
     * and the user can use it directly.
     * <p>
     * For MVP, reset tokens are stored in memory. In production, persist tokens
     * (database/Redis) with expiration and single-use enforcement.
     */
    public String requestPatientReset(String email) {
        Long patientId = patientService.findIdByEmail(email);
        if (patientId == null) {
            throw new IllegalArgumentException("No patient found with email: " + email);
        }

        String token = UUID.randomUUID().toString();
        patientResetTokens.put(token, new ResetToken(patientId, System.currentTimeMillis()));
        // Clean up expired tokens periodically (every 10th call)
        if (patientResetTokens.size() % 10 == 0) {
            cleanExpiredPatientTokens();
        }
        try {
            emailService.sendPasswordResetLink(email, "Patient",
                    baseUrl + "/reset-password?token=" + token);
        } catch (MailException e) {
            patientResetTokens.remove(token);
            log.error("Password reset email delivery failed", e);
            throw new IllegalStateException("Unable to send the password reset email. Check the SMTP configuration.");
        }
        return token;
    }

    /** Admin triggers a password reset for a doctor. A temporary password is
     * generated, set on the doctor's account, and emailed to the doctor.
     */
    public void triggerDoctorReset(Long doctorId, String doctorEmail) {
        var doctor = doctorService.findById(doctorId);
        if (!doctor.getEmail().equalsIgnoreCase(doctorEmail)) {
            throw new IllegalArgumentException("Doctor email does not match the account");
        }
        String tempPassword = UUID.randomUUID().toString().substring(0, 12);

        String token = UUID.randomUUID().toString();
        doctorResetTokens.put(token, new ResetToken(doctorId, System.currentTimeMillis()));
        doctorService.forceResetPassword(doctorId, tempPassword);

        emailService.sendTemporaryPassword(doctor.getEmail(),
                doctor.getFirstName() + " " + doctor.getLastName(), tempPassword);
    }

    /** Patient completes password reset using a valid token. */
    public void completePatientReset(String token, String newPassword) {
        ResetToken resetToken = patientResetTokens.get(token);
        if (resetToken == null || resetToken.isExpired()) {
            patientResetTokens.remove(token);
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        if (!patientResetTokens.remove(token, resetToken)) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        patientService.forceResetPassword(resetToken.userId(), newPassword);
    }

    /** Doctor completes admin-triggered reset using token and temp password. */
    public void completeDoctorReset(String token, String tempPassword, String newPassword) {
        ResetToken resetToken = doctorResetTokens.get(token);
        if (resetToken == null || resetToken.isExpired()) {
            doctorResetTokens.remove(token);
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        if (!doctorResetTokens.remove(token, resetToken)) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        doctorService.changePassword(resetToken.userId(), tempPassword, newPassword);
    }

    /** Clean up expired patient reset tokens (older than 1 hour). */
    private void cleanExpiredPatientTokens() {
        long now = System.currentTimeMillis();
        patientResetTokens.entrySet().removeIf(entry -> {
            return now - entry.getValue().createdAt() > TOKEN_VALIDITY_MS;
        });
        doctorResetTokens.entrySet().removeIf(entry -> {
            return now - entry.getValue().createdAt() > TOKEN_VALIDITY_MS;
        });
    }

    private record ResetToken(Long userId, long createdAt) {
        private boolean isExpired() {
            long age = System.currentTimeMillis() - createdAt;
            return age < 0 || age > TOKEN_VALIDITY_MS;
        }
    }
}
