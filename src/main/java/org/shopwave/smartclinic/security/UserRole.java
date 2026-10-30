package org.shopwave.smartclinic.security;

/**
 * Roles used throughout the clinic application.
 * <p>
 * Three roles mirror the three user types in {@code user_stories.md}:
 * ADMIN, DOCTOR, PATIENT. Each role maps to its own entity table
 * (Admin, Doctor, Patient) and its own dashboard.
 */
public enum UserRole {
    ROLE_ADMIN,
    ROLE_DOCTOR,
    ROLE_PATIENT
}
