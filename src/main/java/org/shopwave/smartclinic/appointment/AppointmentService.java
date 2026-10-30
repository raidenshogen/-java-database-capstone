package org.shopwave.smartclinic.appointment;

import org.shopwave.smartclinic.dto.AppointmentRequest;
import org.shopwave.smartclinic.dto.AppointmentResponse;
import org.shopwave.smartclinic.model.Appointment;
import org.shopwave.smartclinic.model.AppointmentStatus;
import org.shopwave.smartclinic.repository.AppointmentRepository;
import org.shopwave.smartclinic.repository.DoctorRepository;
import org.shopwave.smartclinic.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for {@link Appointment} booking and lifecycle.
 * <p>
 * Key business rules (from {@code schema-design.md} and {@code user_stories.md}):
 * <ul>
 *   <li>Double-booking prevention: a doctor cannot have two SCHEDULED
 *       appointments at the same date/time. Enforced in a transaction with
 *       {@code SELECT ... FOR UPDATE} semantics via the repository query.</li>
 *   <li>Terminal statuses (COMPLETED, CANCELLED, NO_SHOW) require explicit
 *       manual action — never changed automatically by the clock.</li>
 *   <li>Cancelled slots reopen immediately for re-booking (no UNIQUE constraint
 *       on doctor/date/time because cancelled rows must not block slots).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    public List<AppointmentResponse> findByPatient(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(
                        patientId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponse> findByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorIdOrderByAppointmentDateDescAppointmentTimeDesc(
                        doctorId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponse> findScheduledByDoctorForDate(Long doctorId,
                                                                  LocalDate date) {
        return appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusOrderByAppointmentTime(
                        doctorId, date, AppointmentStatus.SCHEDULED).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Books a new appointment. Runs in a transaction to prevent race-condition
     * double-booking (see {@code schema-design.md} "Concurrency Consideration").
     */
    @Transactional
    public AppointmentResponse book(AppointmentRequest request) {
        PatientAndDoctor pd = loadPatientAndDoctor(request);

        // 1. Doctor double-booking: same doctor, same date/time, SCHEDULED
        boolean doctorConflict = appointmentRepository.findScheduledByDoctorAndDateTime(
                pd.doctor.getId(),
                request.getAppointmentDate(),
                request.getAppointmentTime(),
                AppointmentStatus.SCHEDULED
        ).isPresent();

        if (doctorConflict) {
            throw new IllegalStateException(
                "Doctor " + pd.doctor.getName() + " is already booked at "
                + request.getAppointmentDate() + " " + request.getAppointmentTime()
                + ". Please choose another slot.");
        }

        // 2. Patient double-booking: patient already has a SCHEDULED appointment
        boolean patientConflict = appointmentRepository.findScheduledByPatient(
                pd.patient.getId(),
                AppointmentStatus.SCHEDULED
        ).isPresent();

        if (patientConflict) {
            throw new IllegalStateException(
                "Patient " + pd.patient.getName() + " already has a scheduled appointment. "
                + "Cancel or complete it before booking a new one.");
        }

        Appointment appointment = Appointment.builder()
                .patient(pd.patient)
                .doctor(pd.doctor)
                .appointmentDate(request.getAppointmentDate())
                .appointmentTime(request.getAppointmentTime())
                .status(AppointmentStatus.SCHEDULED)
                .build();

        return toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse markCompleted(Long appointmentId, Long doctorId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Appointment not found with id: " + appointmentId));

        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new IllegalStateException("Appointment does not belong to this doctor");
        }
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new IllegalStateException(
                "Only SCHEDULED appointments can be marked COMPLETED; current: "
                + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        return toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse markNoShow(Long appointmentId, Long doctorId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Appointment not found with id: " + appointmentId));

        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new IllegalStateException("Appointment does not belong to this doctor");
        }
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new IllegalStateException(
                "Only SCHEDULED appointments can be marked NO_SHOW; current: "
                + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.NO_SHOW);
        return toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse cancelByDoctor(Long appointmentId, Long doctorId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Appointment not found with id: " + appointmentId));

        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new IllegalStateException("Appointment does not belong to this doctor");
        }
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new IllegalStateException(
                "Only SCHEDULED appointments can be cancelled; current: "
                + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        return toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse cancelByPatient(Long appointmentId, Long patientId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Appointment not found with id: " + appointmentId));

        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new IllegalStateException("Appointment does not belong to this patient");
        }
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new IllegalStateException(
                "Only SCHEDULED appointments can be cancelled; current: "
                + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        return toResponse(appointmentRepository.save(appointment));
    }

    private PatientAndDoctor loadPatientAndDoctor(AppointmentRequest request) {
        // Auto-assign doctor: patient books by department in MVP, but here
        // we require the patientId and derive the doctor from assignedDoctorId
        // or the first available doctor. For MVP simplicity, the doctorId is
        // not in the request; we resolve via patient's assigned doctor.
        var patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found with id: " + request.getPatientId()));

        var doctor = doctorRepository.findById(patient.getAssignedDoctorId())
                .orElseThrow(() -> new IllegalStateException(
                        "Patient has no assigned doctor. Contact admin."));

        return new PatientAndDoctor(patient, doctor);
    }

    private record PatientAndDoctor(org.shopwave.smartclinic.model.Patient patient,
                                    org.shopwave.smartclinic.model.Doctor doctor) {
    }

    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .patientId(a.getPatient().getId())
                .patientName(a.getPatient().getName())
                .doctorId(a.getDoctor().getId())
                .doctorName(a.getDoctor().getName())
                .appointmentDate(a.getAppointmentDate())
                .appointmentTime(a.getAppointmentTime())
                .status(a.getStatus().name())
                .build();
    }
}
