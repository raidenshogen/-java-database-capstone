package org.shopwave.smartclinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Represents a scheduled visit linking a {@link Patient} and a {@link Doctor}.
 * <p>
 * Design notes (from {@code schema-design.md}):
 * <ul>
 *   <li>Resolves the Patient &lt;-&gt; Doctor many-to-many relationship as a junction table.</li>
 *   <li>Double-booking prevention is enforced in the Service layer (not via a DB UNIQUE constraint),
 *       because cancelled rows must not permanently occupy a slot.</li>
 *   <li>{@code getEndTime()} derives the end time from the appointment start time plus the
 *       doctor's {@code slotDurationMinutes}. Duration lives on {@link Doctor}, not on Appointment.</li>
 * </ul>
 */
@Entity
@Table(name = "appointments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @NotNull
    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @NotNull
    @FutureOrPresent(message = "Appointment time must be in the present or future")
    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Returns the end time of this appointment.
     * <p>
     * Computed as {@code appointmentTime} plus the doctor's {@code slotDurationMinutes}.
     * Returns {@code appointmentTime} plus a 30-minute default if the doctor is not loaded.
     *
     * @return the end time as a {@link LocalDateTime}, never null
     */
    public LocalDateTime getEndTime() {
        int slotMinutes = (doctor != null && doctor.getSlotDurationMinutes() != null)
                ? doctor.getSlotDurationMinutes()
                : 30;
        return appointmentTime.plusMinutes(slotMinutes);
    }
}
