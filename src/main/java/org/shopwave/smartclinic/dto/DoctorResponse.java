package org.shopwave.smartclinic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for exposing a Doctor to the client (never the password hash).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String specialtyName;
    private Integer slotDurationMinutes;
    private boolean active;
}
