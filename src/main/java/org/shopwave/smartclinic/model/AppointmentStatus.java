package org.shopwave.smartclinic.model;

/**
 * Appointment lifecycle status.
 * <p>
 * Mirrors the MySQL {@code ENUM('SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')}
 * column defined in {@code schema-design.md}. Terminal statuses require an explicit
 * manual action — they are never changed automatically by the clock.
 */
public enum AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}
