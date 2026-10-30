package org.shopwave.smartclinic.repository;

import org.shopwave.smartclinic.model.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link Specialty} lookup records.
 */
@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

    Optional<Specialty> findByName(String name);

    boolean existsByName(String name);
}
