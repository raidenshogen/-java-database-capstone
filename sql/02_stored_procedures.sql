-- ============================================================================
-- Smart Clinic — Stored Procedures
--
-- Run from the project root:
--   docker compose exec -T mysql mysql -uroot -prootpassword \
--     < sql/02_stored_procedures.sql
-- ============================================================================

USE smartclinic;

DELIMITER $$

-- ---------------------------------------------------------------------------
-- Procedure 1: GetDailyAppointmentReportByDoctor
-- Returns one row for every active doctor, including doctors with no
-- appointments on the requested date.
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS GetDailyAppointmentReportByDoctor$$

CREATE PROCEDURE GetDailyAppointmentReportByDoctor(IN input_date DATE)
BEGIN
    SELECT
        d.id AS doctor_id,
        d.first_name,
        d.last_name,
        s.name AS specialty,
        COUNT(a.id) AS total_appointments,
        SUM(CASE WHEN a.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed,
        SUM(CASE WHEN a.status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled,
        SUM(CASE WHEN a.status = 'NO_SHOW' THEN 1 ELSE 0 END) AS no_show,
        SUM(CASE WHEN a.status = 'SCHEDULED' THEN 1 ELSE 0 END) AS still_scheduled
    FROM doctors d
        LEFT JOIN specialties s ON s.id = d.specialty_id
        LEFT JOIN appointments a
            ON a.doctor_id = d.id
            AND a.appointment_date = input_date
    WHERE d.is_active = TRUE
    GROUP BY d.id, d.first_name, d.last_name, s.name
    ORDER BY total_appointments DESC, d.id ASC;
END$$

-- ---------------------------------------------------------------------------
-- Procedure 2: GetDoctorWithMostPatientsByMonth
-- "Saw" means COMPLETED appointments. Ties are resolved by total visits and
-- then doctor ID for deterministic output.
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS GetDoctorWithMostPatientsByMonth$$

CREATE PROCEDURE GetDoctorWithMostPatientsByMonth(
    IN input_month INT,
    IN input_year INT
)
BEGIN
    SELECT
        d.id AS doctor_id,
        d.first_name,
        d.last_name,
        COUNT(DISTINCT a.patient_id) AS distinct_patients_seen,
        COUNT(a.id) AS total_completed_visits
    FROM appointments a
        JOIN doctors d ON d.id = a.doctor_id
    WHERE a.status = 'COMPLETED'
        AND MONTH(a.appointment_date) = input_month
        AND YEAR(a.appointment_date) = input_year
    GROUP BY d.id, d.first_name, d.last_name
    ORDER BY distinct_patients_seen DESC,
             total_completed_visits DESC,
             d.id ASC
    LIMIT 1;
END$$

-- ---------------------------------------------------------------------------
-- Procedure 3: GetDoctorWithMostPatientsByYear
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS GetDoctorWithMostPatientsByYear$$

CREATE PROCEDURE GetDoctorWithMostPatientsByYear(IN input_year INT)
BEGIN
    SELECT
        d.id AS doctor_id,
        d.first_name,
        d.last_name,
        COUNT(DISTINCT a.patient_id) AS distinct_patients_seen,
        COUNT(a.id) AS total_completed_visits
    FROM appointments a
        JOIN doctors d ON d.id = a.doctor_id
    WHERE a.status = 'COMPLETED'
        AND YEAR(a.appointment_date) = input_year
    GROUP BY d.id, d.first_name, d.last_name
    ORDER BY distinct_patients_seen DESC,
             total_completed_visits DESC,
             d.id ASC
    LIMIT 1;
END$$

DELIMITER ;
