package org.shopwave.smartclinic.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a clinical prescription stored in MongoDB.
 * <p>
 * Design notes (from {@code schema-design.md}):
 * <ul>
 *   <li>MongoDB is used because prescriptions have a variable-length, nested structure
 *       (a patient may be prescribed 1 medicine or many).</li>
 *   <li>{@code patientId} and {@code doctorId} reference primary keys in MySQL tables.
 *       Cross-database referential integrity is <b>not</b> enforced by either engine —
 *       the Service layer must validate that referenced IDs exist before writing.</li>
 *   <li>{@code patientName} is a convenience snapshot for fast display; {@code patientId}
 *       remains the authoritative reference to MySQL.</li>
 *   <li>{@code medicines} is an array (your request said "medication" singular). Each entry
 *       holds name, dosage, and duration.</li>
 *   <li>Once authored, this collection is treated as an append-only clinical ledger — a
 *       newly assigned doctor creates a new document rather than editing another doctor's.</li>
 * </ul>
 */
@Document(collection = "prescriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prescription {

    @Id
    private String id;

    @NotNull
    private Long appointmentId;

    @NotNull
    private Long doctorId;

    @NotNull
    private Long patientId;

    /**
     * Embedded snapshot of the patient's name and contact at prescription time,
     * for fast UI display without a round-trip to MySQL. The authoritative reference
     * to MySQL is {@link #patientId}.
     */
    private PatientSnapshot patient;

    @NotNull
    @Builder.Default
    private List<Medicine> medicines = new ArrayList<>();

    private String diagnosis;

    private String note;

    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * Embedded patient snapshot stored within a prescription document.
     * Mirrors the embedded {@code patient} object in the schema design.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientSnapshot {

        private String firstName;
        private String lastName;
        private String contact;
    }

    /**
     * A single medicine prescribed within a prescription.
     * Embedded as a sub-document in MongoDB — no separate collection.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Medicine {

        @NotNull
        private String name;

        @NotNull
        private String dosage;

        @NotNull
        private String duration;
    }
}
