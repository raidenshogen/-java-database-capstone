package org.shopwave.smartclinic.contollers;

import org.shopwave.smartclinic.doctor.DoctorService;
import org.shopwave.smartclinic.dto.DoctorRequest;
import org.shopwave.smartclinic.dto.DoctorResponse;
import org.shopwave.smartclinic.security.ClinicUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for doctors.
 * <p>
 * Roles: ADMIN full CRUD; DOCTOR can read own profile; PATIENT can read active doctors.
 */
@RestController
@RequestMapping("/api/doctors")
@PreAuthorize("isAuthenticated()")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public List<DoctorResponse> findAllActive() {
        return doctorService.findAllActive();
    }

    @GetMapping("/{id}")
    public DoctorResponse findById(@PathVariable Long id) {
        return doctorService.findById(id);
    }

    @GetMapping("/specialty/{specialtyId}")
    public List<DoctorResponse> findBySpecialty(@PathVariable Long specialtyId) {
        return doctorService.findBySpecialty(specialtyId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DoctorResponse> create(@RequestBody DoctorRequest request) {
        DoctorResponse created = doctorService.create(request);
        return ResponseEntity.created(URI.create("/api/doctors/" + created.getId()))
                .body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DoctorResponse update(@PathVariable Long id,
                                 @RequestBody DoctorRequest request) {
        return doctorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        doctorService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/working-hours")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> setWorkingHours(@PathVariable Long id,
                                                @RequestParam java.time.LocalTime start,
                                                @RequestParam java.time.LocalTime end,
                                                @RequestParam Integer slotDurationMinutes) {
        doctorService.setWorkingHours(id, start, end, slotDurationMinutes);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns available time slots for a doctor on a specific date.
     * Requires authentication — the caller must be a logged-in user.
     *
     * @param id   the doctor's ID
     * @param date the date to check availability for
     * @param user the currently authenticated user (token validation)
     * @return a structured {@link ResponseEntity} with the list of available slots
     */
    @GetMapping("/{id}/availability")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getAvailability(
            @PathVariable Long id,
            @RequestParam LocalDate date,
            @AuthenticationPrincipal ClinicUserDetails user) {
        if (user == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Authentication required");
            return ResponseEntity.status(401).body(error);
        }

        List<LocalTime> slots = doctorService.getAvailableSlots(id, date);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("doctorId", id);
        response.put("date", date);
        response.put("availableSlots", slots);
        return ResponseEntity.ok(response);
    }
}
