package org.shopwave.smartclinic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for exposing a Specialty to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialtyResponse {

    private Long id;
    private String name;
}
