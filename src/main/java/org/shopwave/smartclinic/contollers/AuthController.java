package org.shopwave.smartclinic.contollers;

import org.shopwave.smartclinic.auth.PasswordResetService;
import org.shopwave.smartclinic.dto.PasswordChangeRequest;
import org.shopwave.smartclinic.doctor.DoctorService;
import org.shopwave.smartclinic.patient.PatientService;
import org.shopwave.smartclinic.security.ClinicUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * Auth-related views and actions.
 * <p>
 * - GET /login — login page (Thymeleaf)
 * - POST /perform-login — handled by Spring Security filter chain
 * - GET /role-redirect — after login, redirects to the role-appropriate dashboard
 * - POST /perform-logout — handled by Spring Security filter chain
 * - POST /api/auth/change-password — self-service password change (REST)
 * - POST /api/auth/forgot-password — patient requests reset link (REST)
 * - POST /api/auth/reset-password — confirm reset with token (REST)
 */
@Controller
public class AuthController {

    private final DoctorService doctorService;
    private final PatientService patientService;
    private final PasswordResetService passwordResetService;

    public AuthController(DoctorService doctorService,
                          PatientService patientService,
                          PasswordResetService passwordResetService) {
        this.doctorService = doctorService;
        this.patientService = patientService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        return "login";
    }

    @GetMapping("/role-redirect")
    public String roleRedirect(@AuthenticationPrincipal ClinicUserDetails user) {
        if (user == null) {
            return "redirect:/login";
        }
        return switch (user.getRole()) {
            case ROLE_ADMIN -> "redirect:/admin/dashboard";
            case ROLE_DOCTOR -> "redirect:/doctor/dashboard";
            case ROLE_PATIENT -> "redirect:/patient/dashboard";
        };
    }

    @PostMapping("/perform-logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response,
                SecurityContextHolder.getContext().getAuthentication());
        return "redirect:/login?logout=true";
    }

    /**
     * Self-service password change for doctors and patients.
     * Validates the current password before setting the new one.
     */
    @PostMapping("/api/auth/change-password")
    @ResponseBody
    public ResponseEntity<?> changePassword(
            @RequestBody PasswordChangeRequest request,
            @AuthenticationPrincipal ClinicUserDetails user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        try {
            switch (user.getRole()) {
                case ROLE_DOCTOR -> doctorService.changePassword(
                        user.getId(), request.getCurrentPassword(), request.getNewPassword());
                case ROLE_PATIENT -> patientService.changePassword(
                        user.getId(), request.getCurrentPassword(), request.getNewPassword());
                case ROLE_ADMIN -> {
                    // Admin password change handled by AdminService (admin-triggered only)
                    throw new IllegalStateException(
                            "Admin password changes must be initiated by another admin");
                }
            }
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * Patient requests a password reset link via email (self-service).
     */
    @PostMapping("/api/auth/forgot-password")
    @ResponseBody
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        try {
            passwordResetService.requestPatientReset(email);
            return ResponseEntity.ok(Map.of("message", "Reset link sent to " + email));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * Patient completes password reset using a token and new password.
     */
    @PostMapping("/api/auth/reset-password")
    @ResponseBody
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token and new password are required"));
        }
        try {
            passwordResetService.completePatientReset(token, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * Admin triggers a doctor password reset. A temporary password is emailed
     * to the doctor.
     */
    @PostMapping("/api/auth/doctor-reset")
    @ResponseBody
    public ResponseEntity<?> triggerDoctorReset(@RequestBody Map<String, String> body,
                                                @AuthenticationPrincipal ClinicUserDetails user) {
        if (user == null || user.getRole() != org.shopwave.smartclinic.security.UserRole.ROLE_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required"));
        }
        Long doctorId;
        try {
            doctorId = Long.valueOf(body.get("doctorId"));
        } catch (NumberFormatException | NullPointerException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valid doctorId is required"));
        }
        String doctorEmail = body.get("email");
        if (doctorEmail == null || doctorEmail.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Doctor email is required"));
        }
        try {
            passwordResetService.triggerDoctorReset(doctorId, doctorEmail);
            return ResponseEntity.ok(Map.of("message", "Temporary password sent to " + doctorEmail));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
