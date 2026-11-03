package org.shopwave.smartclinic.contollers;

import org.shopwave.smartclinic.service.appointment.AppointmentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Doctor dashboard views (Thymeleaf server-rendered).
 * <p>
 * Only users with ROLE_DOCTOR can reach these endpoints.
 */
@Controller
public class DoctorDashboardController {

    private final AppointmentService appointmentService;

    public DoctorDashboardController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/doctor/dashboard")
    public String dashboard(@AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user,
                            Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("displayName", user.getDisplayName());
        model.addAttribute("role", user.getRole().name());
        model.addAttribute("doctorId", user.getId());
        model.addAttribute("today", LocalDate.now());
        return "doctor/dashboard";
    }

    @GetMapping("/doctor/schedule")
    public String schedule(@RequestParam(value = "date", required = false) LocalDate date,
                           @AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user,
                           Model model) {
        LocalDate viewDate = date != null ? date : LocalDate.now();
        model.addAttribute("appointments",
                appointmentService.findScheduledByDoctorForDate(user.getId(), viewDate));
        model.addAttribute("date", viewDate);
        return "doctor/schedule";
    }

    @GetMapping("/doctor/patients")
    public String patients(@AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user,
                           Model model) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("displayName", user.getDisplayName());
        model.addAttribute("doctorId", user.getId());
        return "doctor/patients";
    }
}
