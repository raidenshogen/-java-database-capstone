package org.shopwave.smartclinic.contollers;

import jakarta.validation.Valid;
import org.shopwave.smartclinic.dto.PatientRequest;
import org.shopwave.smartclinic.dto.PatientResponse;
import org.shopwave.smartclinic.service.patient.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/public/patients")
public class PublicPatientController {

    private final PatientService patientService;

    public PublicPatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    public ResponseEntity<PatientResponse> signup(@RequestBody @Valid PatientRequest request) {
        PatientResponse created = patientService.create(request);
        return ResponseEntity.created(URI.create("/api/patients/" + created.getId()))
                .body(created);
    }
}
