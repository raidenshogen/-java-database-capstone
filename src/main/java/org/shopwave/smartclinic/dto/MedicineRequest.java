package org.shopwave.smartclinic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single medicine within a prescription request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineRequest {

    @NotBlank(message = "medicine name is required")
    private String name;

    @NotBlank(message = "dosage is required")
    private String dosage;

    @NotBlank(message = "duration is required")
    private String duration;
}
