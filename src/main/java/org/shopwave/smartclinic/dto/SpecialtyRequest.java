package org.shopwave.smartclinic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating/updating a {@link org.shopwave.smartclinic.model.Specialty}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialtyRequest {

    @NotBlank(message = "specialty name is required")
    private String name;
}
