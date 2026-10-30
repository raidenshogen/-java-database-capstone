package org.shopwave.smartclinic.repository;

import org.shopwave.smartclinic.model.Appointment;
import org.shopwave.smartclinic.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Appointment} records.
 * <p>
 * Double-booking prevention is enforced in the Service layer (see
 * {@code schema-design.md} "Concurrency Consideration"), but the repository
 * provides the queries the service needs.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorIdAndAppointmentDateAndStatusOrderByAppointmentTime(
            Long doctorId, LocalDate appointmentDate, AppointmentStatus status);

    List<Appointment> findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(Long patientId);

    List<Appointment> findByDoctorIdOrderByAppointmentDateDescAppointmentTimeDesc(Long doctorId);

    /**
     * Checks whether a doctor already has a SCHEDULED appointment at the given
     * date/time. Used by the service layer to prevent double-booking.
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.doctor.id = :doctorId
          AND a.appointmentDate = :appointmentDate
          AND a.appointmentTime = :appointmentTime
          AND a.status = :status
    """)
    Optional<Appointment> findScheduledByDoctorAndDateTime(
            @Param("doctorId") Long doctorId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("appointmentTime") LocalDateTime appointmentTime,
            @Param("status") AppointmentStatus status);

    List<Appointment> findByPatientIdAndStatus(Long patientId, AppointmentStatus status);

    /**
     * Checks whether a patient already has a SCHEDULED appointment (any doctor,
     * any department) — used to prevent patient double-booking (US-PAT-05).
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.patient.id = :patientId
          AND a.status = :status
    """)
    Optional<Appointment> findScheduledByPatient(
            @Param("patientId") Long patientId,
            @Param("status") AppointmentStatus status);
}
