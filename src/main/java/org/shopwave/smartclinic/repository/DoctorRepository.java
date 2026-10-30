package org.shopwave.smartclinic.repository;

import org.shopwave.smartclinic.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Doctor} accounts.
 * <p>
 * Supports active/inactive filtering and specialty-based lookups used by the
 * booking engine and admin dashboards.
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findByIsActiveTrue();

    List<Doctor> findBySpecialtyIdAndIsActiveTrue(Integer specialtyId);

    boolean existsByEmail(String email);

    Optional<Doctor> findByEmail(String email);
}
