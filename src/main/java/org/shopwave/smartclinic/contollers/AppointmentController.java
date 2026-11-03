package org.shopwave.smartclinic.contollers;

import org.shopwave.smartclinic.dto.AppointmentRequest;
import org.shopwave.smartclinic.dto.AppointmentResponse;
import org.shopwave.smartclinic.service.appointment.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for appointments.
 * <p>
 * Roles:
 * - PATIENT: book, view own, cancel own
 * - DOCTOR: view own day's schedule, mark COMPLETED/NO_SHOW, cancel own
 * - ADMIN: view all
 */
@RestController
@RequestMapping("/api/appointments")
@PreAuthorize("isAuthenticated()")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/patient/{patientId}")
    public List<AppointmentResponse> findByPatient(
            @PathVariable Long patientId,
            @AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user) {
        if (user.getRole() == org.shopwave.smartclinic.security.UserRole.ROLE_PATIENT
                && !user.getId().equals(patientId)) {
            throw new IllegalStateException("Patients can only view their own appointments");
        }
        if (user.getRole() == org.shopwave.smartclinic.security.UserRole.ROLE_DOCTOR) {
            return appointmentService.findByPatientForDoctor(patientId, user.getId());
        }
        return appointmentService.findByPatient(patientId);
    }

    @GetMapping("/doctor/{doctorId}/today")
    public List<AppointmentResponse> findScheduledTodayForDoctor(
            @PathVariable Long doctorId,
            @RequestParam LocalDate date,
            @AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user) {
        if (user.getRole() == org.shopwave.smartclinic.security.UserRole.ROLE_DOCTOR
                && !user.getId().equals(doctorId)) {
            throw new IllegalStateException("Doctors can only view their own schedule");
        }
        return appointmentService.findScheduledByDoctorForDate(doctorId, date);
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<AppointmentResponse> book(
            @RequestBody @jakarta.validation.Valid AppointmentRequest request,
            @AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user) {
        if (!user.getId().equals(request.getPatientId())) {
            throw new IllegalStateException("Patients can only book for themselves");
        }
        AppointmentResponse created = appointmentService.book(request);
        return ResponseEntity.created(URI.create("/api/appointments/" + created.getId()))
                .body(created);
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    public AppointmentResponse markCompleted(@PathVariable Long id,
                                             @AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user) {
        return appointmentService.markCompleted(id, user.getId());
    }

    @PutMapping("/{id}/no-show")
    @PreAuthorize("hasRole('DOCTOR')")
    public AppointmentResponse markNoShow(@PathVariable Long id,
                                          @AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user) {
        return appointmentService.markNoShow(id, user.getId());
    }

    @PutMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id,
                                      @RequestParam(defaultValue = "patient") String cancelledBy,
                                      @AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user) {
        if ("doctor".equalsIgnoreCase(cancelledBy)) {
            if (user.getRole() != org.shopwave.smartclinic.security.UserRole.ROLE_DOCTOR) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Only doctors can cancel appointments as a doctor");
            }
            return appointmentService.cancelByDoctor(id, user.getId());
        }
        if (user.getRole() != org.shopwave.smartclinic.security.UserRole.ROLE_PATIENT) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only patients can cancel appointments as a patient");
        }
        return appointmentService.cancelByPatient(id, user.getId());
    }
}
