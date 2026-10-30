package org.shopwave.smartclinic.specialty;

import org.shopwave.smartclinic.dto.SpecialtyRequest;
import org.shopwave.smartclinic.dto.SpecialtyResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST controller for specialty lookup records.
 * <p>
 * Roles: ADMIN can create/update/delete; DOCTOR and PATIENT can read.
 */
@RestController
@RequestMapping("/api/specialties")
@PreAuthorize("isAuthenticated()")
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    public SpecialtyController(SpecialtyService specialtyService) {
        this.specialtyService = specialtyService;
    }

    @GetMapping
    public List<SpecialtyResponse> findAll() {
        return specialtyService.findAll();
    }

    @GetMapping("/{id}")
    public SpecialtyResponse findById(@PathVariable Long id) {
        return specialtyService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpecialtyResponse> create(@RequestBody SpecialtyRequest request) {
        SpecialtyResponse created = specialtyService.create(request);
        return ResponseEntity.created(URI.create("/api/specialties/" + created.getId()))
                .body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SpecialtyResponse update(@PathVariable Long id,
                                    @RequestBody SpecialtyRequest request) {
        return specialtyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        specialtyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
