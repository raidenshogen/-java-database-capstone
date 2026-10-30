package org.shopwave.smartclinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a {@link org.shopwave.smartclinic.model.Prescription}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionRequest {

    @NotNull(message = "appointment id is required")
    private Long appointmentId;

    @NotNull(message = "patient id is required")
    private Long patientId;

    @NotNull(message = "diagnosis is required")
    private String diagnosis;

    private String note;

    @NotNull(message = "at least one medicine is required")
    private List<MedicineRequest> medicines;
}
