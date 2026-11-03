package org.shopwave.smartclinic.specification;

import org.shopwave.smartclinic.model.Appointment;
import org.shopwave.smartclinic.model.AppointmentStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Centralized specifications for Appointment queries.
 * <p>
 * Used to prevent code duplication and ensure consistent query patterns
 * across the service layer.
 */
public class AppointmentSpecification {

    /**
     * A doctor has a SCHEDULED appointment at the given date/time.
     */
    public static Specification<Appointment> hasScheduledAppointment(
            Long doctorId, LocalDate appointmentDate, LocalTime appointmentTime) {
        return (root, query, criteriaBuilder) -> {
            Predicate doctorIdMatch = criteriaBuilder.equal(root.get("doctor").get("id"), doctorId);
            Predicate dateMatch = criteriaBuilder.equal(root.get("appointmentDate"), appointmentDate);
            Predicate timeMatch = criteriaBuilder.equal(root.get("appointmentTime"), appointmentTime);
            Predicate statusMatch = criteriaBuilder.equal(root.get("status"), AppointmentStatus.SCHEDULED);
            return criteriaBuilder.and(doctorIdMatch, dateMatch, timeMatch, statusMatch);
        };
    }

    /**
     * A patient has a SCHEDULED appointment (any doctor, any department).
     */
    public static Specification<Appointment> hasScheduledAppointment(Long patientId) {
        return (root, query, criteriaBuilder) -> {
            Predicate patientIdMatch = criteriaBuilder.equal(root.get("patient").get("id"), patientId);
            Predicate statusMatch = criteriaBuilder.equal(root.get("status"), AppointmentStatus.SCHEDULED);
            return criteriaBuilder.and(patientIdMatch, statusMatch);
        };
    }

    /**
     * Find overlapping patient appointments (within a time range).
     */
    public static Specification<Appointment> overlappingAppointments(
            Long patientId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return (root, query, criteriaBuilder) -> {
            Predicate patientMatch = criteriaBuilder.equal(root.get("patient").get("id"), patientId);
            Predicate dateMatch = criteriaBuilder.equal(root.get("appointmentDate"), date);
            // appointmentTime < endTime AND appointmentTime >= startTime
            Predicate beforeEnd = criteriaBuilder.lessThan(root.get("appointmentTime"), endTime);
            Predicate afterStart = criteriaBuilder.greaterThanOrEqualTo(root.get("appointmentTime"), startTime);
            Predicate statusMatch = criteriaBuilder.equal(root.get("status"), AppointmentStatus.SCHEDULED);
            return criteriaBuilder.and(patientMatch, dateMatch, beforeEnd, afterStart, statusMatch);
        };
    }

    /**
     * Appointments by doctor on a specific date with a specific status.
     */
    public static Specification<Appointment> byDoctorAndDate(
            Long doctorId, LocalDate date, AppointmentStatus status) {
        return (root, query, criteriaBuilder) -> {
            Predicate doctorMatch = criteriaBuilder.equal(root.get("doctor").get("id"), doctorId);
            Predicate dateMatch = criteriaBuilder.equal(root.get("appointmentDate"), date);
            Predicate statusMatch = criteriaBuilder.equal(root.get("status"), status);
            return criteriaBuilder.and(doctorMatch, dateMatch, statusMatch);
        };
    }

    public static Specification<Appointment> byDoctorAndDateTime(
            Long doctorId, LocalDate date, LocalTime time, AppointmentStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("doctor").get("id"), doctorId),
                criteriaBuilder.equal(root.get("appointmentDate"), date),
                criteriaBuilder.equal(root.get("appointmentTime"), time),
                criteriaBuilder.equal(root.get("status"), status));
    }

    /**
     * Appointments by patient with a specific status.
     */
    public static Specification<Appointment> byPatientAndStatus(Long patientId, AppointmentStatus status) {
        return (root, query, criteriaBuilder) -> {
            Predicate patientMatch = criteriaBuilder.equal(root.get("patient").get("id"), patientId);
            Predicate statusMatch = criteriaBuilder.equal(root.get("status"), status);
            return criteriaBuilder.and(patientMatch, statusMatch);
        };
    }
}