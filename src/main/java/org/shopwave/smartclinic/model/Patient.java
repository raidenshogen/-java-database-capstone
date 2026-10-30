package org.shopwave.smartclinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a Patient account in the clinic.
 * <p>
 * Design notes (from {@code schema-design.md}):
 * <ul>
 *   <li>Name is split into {@code firstName} and {@code lastName} (not a single "name" field).</li>
 *   <li>{@code phone} and {@code dateOfBirth} are required by the schema but were not in your request.</li>
 *   <li>{@code address} is <b>not</b> present in the schema and has been intentionally omitted.</li>
 *   <li>{@code assignedDoctorId} is nullable and implements soft-delete-safe reassignment
 *       ({@code ON DELETE SET NULL}).</li>
 *   <li>Soft delete via {@code isActive}; deactivated patients are hidden from active lists but
 *       their clinical history is preserved.</li>
 * </ul>
 */
@Entity
@Table(name = "patients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotNull
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotNull
    @Email
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotNull
    @Column(name = "password_hash", nullable = false, length = 255)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwordHash;

    @NotNull
    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "assigned_doctor_id")
    private Long assignedDoctorId;

    @NotNull
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Convenience accessor combining first and last name.
     * <p>
     * The schema stores first and last name separately; this is a read-only helper,
     * not a persisted column.
     *
     * @return the patient's full name, e.g. "Maria Gomez"
     */
    public String getName() {
        return firstName + " " + lastName;
    }
}
