package org.shopwave.smartclinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a Doctor account in the clinic.
 * <p>
 * Design notes (from {@code schema-design.md}):
 * <ul>
 *   <li>Name is split into {@code firstName} and {@code lastName} (not a single "name" field).</li>
 *   <li>{@code specialty} is a foreign key to the {@code specialties} lookup table, stored here as
 *       {@code specialtyId}. A {@link Specialty} entity does not exist yet in the codebase.</li>
 *   <li>{@code availableTimes} is not a stored column — it is derived from
 *       {@code workingHoursStart}, {@code workingHoursEnd}, and {@code slotDurationMinutes}.</li>
 *   <li>Soft delete via {@code isActive}; deactivated doctors are hidden from active listings but
 *       their history is preserved.</li>
 * </ul>
 */
@Entity
@Table(name = "doctors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Doctor {

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
    @Column(name = "specialty_id", nullable = false)
    private Integer specialtyId;

    /**
     * Navigation relationship to the specialty lookup table.
     * <p>
     * The column is owned by {@link #specialtyId}; this reference is read-only
     * from the persistence side (insertable=false, updatable=false) to avoid
     * two fields fighting over the same column.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "specialty_id", insertable = false, updatable = false)
    private Specialty specialty;

    @NotNull
    @Column(name = "working_hours_start", nullable = false)
    @Builder.Default
    private LocalTime workingHoursStart = LocalTime.of(9, 0);

    @NotNull
    @Column(name = "working_hours_end", nullable = false)
    @Builder.Default
    private LocalTime workingHoursEnd = LocalTime.of(17, 0);

    @NotNull
    @Positive(message = "slot_duration_minutes must be greater than 0")
    @Column(name = "slot_duration_minutes", nullable = false)
    @Builder.Default
    private Integer slotDurationMinutes = 30;

    /**
     * Available time slots for this doctor, derived from
     * {@code workingHoursStart}, {@code workingHoursEnd}, and
     * {@code slotDurationMinutes}. Not persisted — computed at runtime
     * by {@link org.shopwave.smartclinic.doctor.DoctorService}.
     */
    @Transient
    private List<LocalTime> availableTimes;

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
     * @return the doctor's full name, e.g. "John Doe"
     */
    public String getName() {
        return firstName + " " + lastName;
    }
}
