package org.shopwave.smartclinic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for exposing a Prescription to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponse {

    private String id;
    private Long appointmentId;
    private Long doctorId;
    private Long patientId;
    private String patientFirstName;
    private String patientLastName;
    private String patientContact;
    private List<MedicineResponse> medicines;
    private String diagnosis;
    private String note;
    private String createdAt;
}
