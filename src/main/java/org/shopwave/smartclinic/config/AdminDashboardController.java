package org.shopwave.smartclinic.config;

import org.shopwave.smartclinic.doctor.DoctorService;
import org.shopwave.smartclinic.patient.PatientService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Admin dashboard views (Thymeleaf server-rendered).
 * <p>
 * Only users with ROLE_ADMIN can reach these endpoints.
 */
@Controller
public class AdminDashboardController {

    private final DoctorService doctorService;
    private final PatientService patientService;

    public AdminDashboardController(DoctorService doctorService,
                                    PatientService patientService) {
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(@AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user,
                            Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("role", user.getRole().name());
        model.addAttribute("doctors", doctorService.findAllActive());
        model.addAttribute("patients", patientService.findAllActive());
        return "admin/dashboard";
    }

    @GetMapping("/admin/doctors")
    public String doctors(Model model) {
        model.addAttribute("doctors", doctorService.findAllActive());
        return "admin/doctors";
    }

    @GetMapping("/admin/patients")
    public String patients(Model model) {
        model.addAttribute("patients", patientService.findAllActive());
        return "admin/patients";
    }
}
