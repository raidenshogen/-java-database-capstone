package org.shopwave.smartclinic.contollers;

import org.shopwave.smartclinic.dto.PatientRequest;
import org.shopwave.smartclinic.dto.PatientResponse;
import org.shopwave.smartclinic.security.ClinicUserDetails;
import org.shopwave.smartclinic.service.patient.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST controller for patients (Admin-side management).
 * <p>
 * Roles: ADMIN full CRUD; DOCTOR/PATIENT can read active patients.
 */
@RestController
@RequestMapping("/api/patients")
@PreAuthorize("isAuthenticated()")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public List<PatientResponse> findAllActive() {
        return patientService.findAllActive();
    }

    @GetMapping("/{id}")
    public PatientResponse findById(@PathVariable Long id,
                                    @AuthenticationPrincipal ClinicUserDetails user) {
        if (user.getRole() == org.shopwave.smartclinic.security.UserRole.ROLE_DOCTOR
                && !patientService.isAssignedToDoctor(id, user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Doctors can only view their assigned patients");
        }
        if (user.getRole() == org.shopwave.smartclinic.security.UserRole.ROLE_PATIENT
                && !user.getId().equals(id)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Patients can only view their own profile");
        }
        return patientService.findById(id);
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public List<PatientResponse> findAssignedPatients(
            @PathVariable Long doctorId,
            @AuthenticationPrincipal ClinicUserDetails user) {
        if (!user.getId().equals(doctorId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Doctors can only view their assigned patients");
        }
        return patientService.findActiveByDoctor(doctorId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientResponse> create(@RequestBody @jakarta.validation.Valid PatientRequest request) {
        PatientResponse created = patientService.create(request);
        return ResponseEntity.created(URI.create("/api/patients/" + created.getId()))
                .body(created);
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PatientResponse update(@PathVariable Long id,
                                  @RequestBody @jakarta.validation.Valid PatientRequest request) {
        return patientService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        patientService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
