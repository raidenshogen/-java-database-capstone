package org.shopwave.smartclinic.prescription;

import org.shopwave.smartclinic.dto.MedicineRequest;
import org.shopwave.smartclinic.dto.MedicineResponse;
import org.shopwave.smartclinic.dto.PrescriptionRequest;
import org.shopwave.smartclinic.dto.PrescriptionResponse;
import org.shopwave.smartclinic.model.Prescription;
import org.shopwave.smartclinic.repository.AppointmentRepository;
import org.shopwave.smartclinic.repository.PatientRepository;
import org.shopwave.smartclinic.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for {@link Prescription} documents stored in MongoDB.
 * <p>
 * Business rules (from {@code schema-design.md}):
 * <ul>
 *   <li>A doctor can only create a prescription for a patient assigned to them.</li>
 *   <li>A doctor can only edit/delete prescriptions they personally authored
 *       ({@code doctorId}). Reassigned doctors create new documents — never
 *       edit another doctor's work (append-only clinical ledger).</li>
 *   <li>{@code patientId} and {@code doctorId} reference MySQL primary keys.
 *       Cross-database integrity is validated here before writing to MongoDB.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               PatientRepository patientRepository,
                               AppointmentRepository appointmentRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public List<PrescriptionResponse> findByPatient(Long patientId) {
        patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + patientId));
        return prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<PrescriptionResponse> findByDoctor(Long doctorId) {
        return prescriptionRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PrescriptionResponse findById(String id) {
        Prescription p = prescriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prescription not found with id: " + id));
        return toResponse(p);
    }

    @Transactional
    public PrescriptionResponse create(PrescriptionRequest request, Long doctorId) {
        // Validate cross-DB references exist in MySQL
        patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + request.getPatientId()));
        appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Appointment not found with id: " + request.getAppointmentId()));

        // Doctor must be the patient's assigned doctor
        var patient = patientRepository.findById(request.getPatientId())
                .orElseThrow();
        if (!patient.getAssignedDoctorId().equals(doctorId)) {
            throw new IllegalStateException(
                "Only the patient's assigned doctor can create a prescription");
        }

        Prescription prescription = Prescription.builder()
                .appointmentId(request.getAppointmentId())
                .doctorId(doctorId)
                .patientId(request.getPatientId())
                .patient(Prescription.PatientSnapshot.builder()
                        .firstName(patient.getFirstName())
                        .lastName(patient.getLastName())
                        .contact(patient.getPhone())
                        .build())
                .medicines(request.getMedicines().stream()
                        .map(m -> Prescription.Medicine.builder()
                                .name(m.getName())
                                .dosage(m.getDosage())
                                .duration(m.getDuration())
                                .build())
                        .collect(Collectors.toList()))
                .diagnosis(request.getDiagnosis())
                .note(request.getNote())
                .build();

        return toResponse(prescriptionRepository.save(prescription));
    }

    @Transactional
    public PrescriptionResponse update(String id, PrescriptionRequest request, Long doctorId) {
        Prescription existing = prescriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prescription not found with id: " + id));

        // Only the authoring doctor can edit (append-only ledger rule)
        if (!existing.getDoctorId().equals(doctorId)) {
            throw new IllegalStateException(
                "You can only edit prescriptions you personally created");
        }

        existing.setDiagnosis(request.getDiagnosis());
        existing.setNote(request.getNote());
        existing.setMedicines(request.getMedicines().stream()
                .map(m -> Prescription.Medicine.builder()
                        .name(m.getName())
                        .dosage(m.getDosage())
                        .duration(m.getDuration())
                        .build())
                .collect(Collectors.toList()));

        return toResponse(prescriptionRepository.save(existing));
    }

    @Transactional
    public void delete(String id, Long doctorId) {
        Prescription existing = prescriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prescription not found with id: " + id));

        if (!existing.getDoctorId().equals(doctorId)) {
            throw new IllegalStateException(
                "You can only delete prescriptions you personally created");
        }

        prescriptionRepository.deleteById(id);
    }

    private PrescriptionResponse toResponse(Prescription p) {
        return PrescriptionResponse.builder()
                .id(p.getId())
                .appointmentId(p.getAppointmentId())
                .doctorId(p.getDoctorId())
                .patientId(p.getPatientId())
                .patientFirstName(p.getPatient() != null ? p.getPatient().getFirstName() : null)
                .patientLastName(p.getPatient() != null ? p.getPatient().getLastName() : null)
                .patientContact(p.getPatient() != null ? p.getPatient().getContact() : null)
                .medicines(p.getMedicines().stream()
                        .map(m -> MedicineResponse.builder()
                                .name(m.getName())
                                .dosage(m.getDosage())
                                .duration(m.getDuration())
                                .build())
                        .collect(Collectors.toList()))
                .diagnosis(p.getDiagnosis())
                .note(p.getNote())
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null)
                .build();
    }
}
