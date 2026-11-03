package org.shopwave.smartclinic.config;

import org.shopwave.smartclinic.service.doctor.DoctorService;
import org.shopwave.smartclinic.service.patient.PatientService;
import org.shopwave.smartclinic.service.specialty.SpecialtyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;

/**
 * Admin dashboard views (Thymeleaf server-rendered).
 * <p>
 * Only users with ROLE_ADMIN can reach these endpoints.
 */
@Controller
public class AdminDashboardController {

    private final DoctorService doctorService;
    private final PatientService patientService;
    private final SpecialtyService specialtyService;

    public AdminDashboardController(DoctorService doctorService,
                                    PatientService patientService,
                                    SpecialtyService specialtyService) {
        this.doctorService = doctorService;
        this.patientService = patientService;
        this.specialtyService = specialtyService;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(@AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user,
                            Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("displayName", user.getDisplayName());
        model.addAttribute("role", user.getRole().name());
        model.addAttribute("doctors", doctorService.findAllActive());
        model.addAttribute("patients", patientService.findAllActive());
        model.addAttribute("specialties", specialtyService.findAll());
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

    @GetMapping("/api/admin/statistics/patients-by-specialty")
    @ResponseBody
    public List<org.shopwave.smartclinic.dto.SpecialtyStatisticsResponse> patientStatistics() {
        return patientService.activePatientStatisticsBySpecialty();
    }
}
