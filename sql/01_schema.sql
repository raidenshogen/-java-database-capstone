-- ============================================================================
-- Smart Clinic Management System — MySQL bootstrap script
-- Source of truth: schema-design.md (repo root)
--
-- Idempotent: safe to re-run. Existing tables are left untouched.
-- Usage (from a terminal):
--   mysql -u root -p < sql/01_schema.sql
-- ============================================================================

CREATE DATABASE IF NOT EXISTS smartclinic
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE smartclinic;

-- ----------------------------------------------------------------------------
-- Table: specialties (lookup — must exist before doctors)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS specialties (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(100) NOT NULL UNIQUE
);

-- ----------------------------------------------------------------------------
-- Table: admin (single administrative account)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- Table: doctors
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS doctors (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name              VARCHAR(100) NOT NULL,
    last_name               VARCHAR(100) NOT NULL,
    email                   VARCHAR(150) NOT NULL UNIQUE,
    password_hash           VARCHAR(255) NOT NULL,
    specialty_id            INT NOT NULL,
    working_hours_start     TIME NOT NULL DEFAULT '09:00:00',
    working_hours_end       TIME NOT NULL DEFAULT '17:00:00',
    slot_duration_minutes   INT NOT NULL DEFAULT 30,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_doctor_specialty
        FOREIGN KEY (specialty_id) REFERENCES specialties(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_slot_duration
        CHECK (slot_duration_minutes > 0),

    CONSTRAINT chk_working_hours
        CHECK (working_hours_end > working_hours_start)
);

-- ----------------------------------------------------------------------------
-- Table: patients
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS patients (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(150) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    phone               VARCHAR(20) NOT NULL,
    date_of_birth       DATE,
    assigned_doctor_id  BIGINT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_patient_assigned_doctor
        FOREIGN KEY (assigned_doctor_id) REFERENCES doctors(id)
        ON DELETE SET NULL,

    -- Recommended in schema-design.md: doctor dashboard queries
    -- "all patients assigned to me" (MySQL 8 has no CREATE INDEX IF NOT EXISTS,
    -- so the index lives inside the CREATE TABLE to stay idempotent)
    INDEX idx_patients_assigned_doctor (assigned_doctor_id)
);

-- ----------------------------------------------------------------------------
-- Table: appointments (junction table resolving Patient <-> Doctor many-to-many)
-- Note: deliberately NO UNIQUE constraint on (doctor_id, appointment_date,
-- appointment_time) — cancelled rows must not block their slot. Double-booking
-- prevention is enforced in the Service layer (see schema-design.md).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS appointments (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id          BIGINT NOT NULL,
    doctor_id           BIGINT NOT NULL,
    appointment_date    DATE NOT NULL,
    appointment_time    TIME NOT NULL,
    status              ENUM('SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')
                        NOT NULL DEFAULT 'SCHEDULED',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_appointment_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_appointment_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctors(id)
        ON DELETE RESTRICT,

    -- Recommended in schema-design.md: constant availability checks
    INDEX idx_appointments_doctor_date (doctor_id, appointment_date, status)
);

-- ----------------------------------------------------------------------------
-- Table: doctor_blocked_slots
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS doctor_blocked_slots (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id       BIGINT NOT NULL,
    start_datetime  DATETIME NOT NULL,
    end_datetime    DATETIME NOT NULL,
    reason          VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_blocked_slot_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctors(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_blocked_range
        CHECK (end_datetime > start_datetime)
);
