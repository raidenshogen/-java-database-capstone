package org.shopwave.smartclinic.repository;

import org.shopwave.smartclinic.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for the single {@link Admin} account.
 * <p>
 * Design note: only one Admin row is expected (seeded at startup). Provides
 * lookup by username for authentication.
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
