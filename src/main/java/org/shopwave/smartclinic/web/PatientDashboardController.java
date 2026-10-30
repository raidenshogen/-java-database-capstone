package org.shopwave.smartclinic.web;

import org.shopwave.smartclinic.appointment.AppointmentService;
import org.shopwave.smartclinic.prescription.PrescriptionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Patient dashboard views (Thymeleaf server-rendered).
 * <p>
 * Only users with ROLE_PATIENT can reach these endpoints.
 */
@Controller
public class PatientDashboardController {

    private final AppointmentService appointmentService;
    private final PrescriptionService prescriptionService;

    public PatientDashboardController(AppointmentService appointmentService,
                                      PrescriptionService prescriptionService) {
        this.appointmentService = appointmentService;
        this.prescriptionService = prescriptionService;
    }

    @GetMapping("/patient/dashboard")
    public String dashboard(@AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user,
                            Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("role", user.getRole().name());
        model.addAttribute("appointments",
                appointmentService.findByPatient(user.getId()));
        model.addAttribute("prescriptions",
                prescriptionService.findByPatient(user.getId()));
        return "patient/dashboard";
    }
}
