package org.shopwave.smartclinic.repository;

import org.shopwave.smartclinic.model.Prescription;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Prescription} documents stored in MongoDB.
 * <p>
 * Cross-database note: {@code patientId} and {@code doctorId} reference primary
 * keys in MySQL tables. Referential integrity is enforced by the Service layer,
 * not by MongoDB.
 */
@Repository
public interface PrescriptionRepository extends MongoRepository<Prescription, String> {

    List<Prescription> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<Prescription> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);

    boolean existsById(String id);
}
