package org.shopwave.smartclinic.web;

import org.shopwave.smartclinic.service.appointment.AppointmentService;
import org.shopwave.smartclinic.service.prescription.PrescriptionService;
import org.shopwave.smartclinic.service.specialty.SpecialtyService;
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
    private final SpecialtyService specialtyService;

    public PatientDashboardController(AppointmentService appointmentService,
                                      PrescriptionService prescriptionService,
                                      SpecialtyService specialtyService) {
        this.appointmentService = appointmentService;
        this.prescriptionService = prescriptionService;
        this.specialtyService = specialtyService;
    }

    @GetMapping("/patient/dashboard")
    public String dashboard(@AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user,
                            Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("displayName", user.getDisplayName());
        model.addAttribute("role", user.getRole().name());
        model.addAttribute("patientId", user.getId());
        model.addAttribute("specialties", specialtyService.findAll());
        model.addAttribute("appointments",
                appointmentService.findByPatient(user.getId()));
        model.addAttribute("prescriptions",
                prescriptionService.findByPatient(user.getId()));
        return "patient/dashboard";
    }
}
