-- ============================================================================
-- Smart Clinic — Stored Procedures
--
-- Add your procedures below. Run the file after editing:
--   mysql -u root -p < sql/02_stored_procedures.sql
-- (or open it in MySQL Workbench and execute)
--
-- Template:
--   DELIMITER $$
--   CREATE PROCEDURE procedure_name(IN param INT)
--   BEGIN
--       SELECT ...;
--   END$$
--   DELIMITER ;
--
-- Re-creating an existing procedure needs DROP PROCEDURE IF EXISTS first:
--   DROP PROCEDURE IF EXISTS procedure_name$$
-- ============================================================================

USE smartclinic;

DELIMITER $$

-- ---------------------------------------------------------------------------
-- Example (commented out — uncomment, rename, and adapt as your first
-- procedure). This one counts a doctor's SCHEDULED appointments on a date:
--
-- DROP PROCEDURE IF EXISTS sp_count_scheduled_appointments$$
-- CREATE PROCEDURE sp_count_scheduled_appointments(
--     IN p_doctor_id BIGINT,
--     IN p_date DATE
-- )
-- BEGIN
--     SELECT COUNT(*) AS scheduled_count
--     FROM appointments
--     WHERE doctor_id = p_doctor_id
--       AND appointment_date = p_date
--       AND status = 'SCHEDULED';
-- END$$

DELIMITER ;
