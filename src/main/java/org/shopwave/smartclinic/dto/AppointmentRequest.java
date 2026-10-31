package org.shopwave.smartclinic.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for creating an {@link org.shopwave.smartclinic.model.Appointment}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequest {

    @NotNull(message = "patient id is required")
    private Long patientId;

    @NotNull(message = "appointment date is required")
    private LocalDate appointmentDate;

    @NotNull(message = "appointment time is required")
    private LocalTime appointmentTime;
}
