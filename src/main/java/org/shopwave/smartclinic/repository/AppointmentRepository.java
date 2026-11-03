package org.shopwave.smartclinic.repository;

import org.shopwave.smartclinic.model.Appointment;
import org.shopwave.smartclinic.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Repository for {@link Appointment} records.
 * <p>
 * Double-booking prevention is enforced in the Service layer (see
 * {@code schema-design.md} "Concurrency Consideration"), but the repository
 * provides the queries the service needs.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long>,
        JpaSpecificationExecutor<Appointment> {

    List<Appointment> findByDoctorIdAndAppointmentDateAndStatusOrderByAppointmentTime(
            Long doctorId, LocalDate appointmentDate, AppointmentStatus status);

    List<Appointment> findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(Long patientId);

    List<Appointment> findByDoctorIdOrderByAppointmentDateDescAppointmentTimeDesc(Long doctorId);

    long countByDoctorIdAndAppointmentDateAndStatus(
            Long doctorId, LocalDate appointmentDate, AppointmentStatus status);

    /**
     * Checks whether a doctor already has a SCHEDULED appointment at the given
     * date/time. Used by the service layer to prevent double-booking.
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.patient.id = :patientId
          AND a.doctor.specialtyId = :specialtyId
        ORDER BY a.appointmentDate DESC, a.appointmentTime DESC
    """)
    List<Appointment> findByPatientIdAndDoctorSpecialtyId(
            @Param("patientId") Long patientId,
            @Param("specialtyId") Long specialtyId);

    /**
     * Checks whether a patient already has a SCHEDULED appointment (any doctor,
     * any department) — used to prevent patient double-booking (US-PAT-05).
     */
}
