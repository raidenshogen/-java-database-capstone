package org.shopwave.smartclinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

/**
 * Represents a clinic department/specialty (e.g. Cardiology, Dermatology).
 * <p>
 * Design notes (from {@code schema-design.md}):
 * <ul>
 *   <li>Normalized lookup table — specialty names live here once, referenced by
 *       {@link Doctor#specialtyId} via foreign key. Prevents inconsistent spelling
 *       of the same specialty across hundreds of doctor rows.</li>
 *   <li>Admin can add/rename a department in one place; the change is reflected
 *       everywhere through the FK.</li>
 *   <li>{@code ON DELETE RESTRICT} on the doctor side prevents deleting a specialty
 *       that still has doctors assigned.</li>
 * </ul>
 */
@Entity
@Table(name = "specialties")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Specialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
