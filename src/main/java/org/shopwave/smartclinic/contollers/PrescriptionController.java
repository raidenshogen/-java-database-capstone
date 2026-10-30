package org.shopwave.smartclinic.contollers;

import org.shopwave.smartclinic.dto.PrescriptionRequest;
import org.shopwave.smartclinic.dto.PrescriptionResponse;
import org.shopwave.smartclinic.prescription.PrescriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST controller for prescriptions (MongoDB-backed).
 * <p>
 * Roles:
 * - DOCTOR: create, view history, edit/delete own prescriptions
 * - PATIENT: view own prescription history
 */
@RestController
@RequestMapping("/api/prescriptions")
@PreAuthorize("isAuthenticated()")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping("/patient/{patientId}")
    public List<PrescriptionResponse> findByPatient(
            @PathVariable Long patientId,
            @AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user) {
        if (user.getRole() == org.shopwave.smartclinic.security.UserRole.ROLE_PATIENT
                && !user.getId().equals(patientId)) {
            throw new IllegalStateException("Patients can only view their own prescriptions");
        }
        return prescriptionService.findByPatient(patientId);
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public List<PrescriptionResponse> findByDoctor(@PathVariable Long doctorId,
                                                   @AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user) {
        if (!user.getId().equals(doctorId)) {
            throw new IllegalStateException("Doctors can only view their own prescriptions");
        }
        return prescriptionService.findByDoctor(doctorId);
    }

    @GetMapping("/{id}")
    public PrescriptionResponse findById(@PathVariable String id,
                                         @AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user) {
        return prescriptionService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PrescriptionResponse> create(
            @RequestBody PrescriptionRequest request,
            @AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user) {
        PrescriptionResponse created = prescriptionService.create(request, user.getId());
        return ResponseEntity.created(URI.create("/api/prescriptions/" + created.getId()))
                .body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public PrescriptionResponse update(@PathVariable String id,
                                       @RequestBody PrescriptionRequest request,
                                       @AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user) {
        return prescriptionService.update(id, request, user.getId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       @AuthenticationPrincipal org.shopwave.smartclinic.security.ClinicUserDetails user) {
        prescriptionService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
