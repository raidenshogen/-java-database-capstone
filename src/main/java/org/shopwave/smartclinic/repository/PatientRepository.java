package org.shopwave.smartclinic.repository;

import org.shopwave.smartclinic.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Patient} accounts.
 * <p>
 * Supports active/inactive filtering and doctor-assignment lookups.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    List<Patient> findByIsActiveTrue();

    List<Patient> findByAssignedDoctorIdAndIsActiveTrue(Long assignedDoctorId);

    boolean existsByEmail(String email);

    Optional<Patient> findByEmail(String email);

    /**
     * Retrieves a patient by either email or phone number.
     * Used for login lookup where the user may provide either identifier.
     */
    @Query("SELECT p FROM Patient p WHERE p.email = :email OR p.phone = :phone")
    Optional<Patient> findByEmailOrPhone(@Param("email") String email, @Param("phone") String phone);
}
