package org.shopwave.smartclinic.email;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends simple text emails via Spring Mail.
 * <p>
 * Used for:
 * <ul>
 *   <li>Doctor-initiated appointment cancellation — patient notified immediately (US-DOC-07)</li>
 *   <li>Admin-triggered doctor password reset (US-ADM-10)</li>
 *   <li>Patient self-service password reset (US-PAT-07)</li>
 * </ul>
 * <p>
 * Configure SMTP via {@code spring.mail.*} properties in {@code application.properties}.
 * Defaults to Gmail SMTP on port 587 with STARTTLS.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendSimpleMail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    /** Notify a patient that their doctor cancelled an appointment. */
    public void sendAppointmentCancelled(String patientEmail, String patientName,
                                         String doctorName, String appointmentDateTime) {
        sendSimpleMail(
            patientEmail,
            "Appointment Cancelled — SmartClinic",
            "Dear " + patientName + ",\n\n"
            + "Your appointment with Dr. " + doctorName + " on "
            + appointmentDateTime + " has been cancelled by the doctor.\n"
            + "The slot is now available for re-booking.\n\n"
            + "— SmartClinic"
        );
    }

    /** Send a temporary password to a doctor (admin-triggered reset). */
    public void sendTemporaryPassword(String doctorEmail, String doctorName,
                                      String tempPassword) {
        sendSimpleMail(
            doctorEmail,
            "Temporary Password — SmartClinic",
            "Dear " + doctorName + ",\n\n"
            + "Your temporary password is: " + tempPassword + "\n"
            + "Please log in and change it immediately.\n\n"
            + "— SmartClinic Admin"
        );
    }

    /** Send a password reset link/token to a patient. */
    public void sendPasswordResetLink(String patientEmail, String patientName,
                                      String resetLink) {
        sendSimpleMail(
            patientEmail,
            "Password Reset — SmartClinic",
            "Dear " + patientName + ",\n\n"
            + "Click the link below to reset your password:\n"
            + resetLink + "\n\n"
            + "If you did not request this, ignore this email.\n\n"
            + "— SmartClinic"
        );
    }
}
