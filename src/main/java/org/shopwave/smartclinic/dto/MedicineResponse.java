package org.shopwave.smartclinic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single medicine within a prescription response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineResponse {

    private String name;
    private String dosage;
    private String duration;
}
