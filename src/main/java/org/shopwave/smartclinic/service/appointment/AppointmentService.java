package org.shopwave.smartclinic.service.appointment;

import org.shopwave.smartclinic.dto.AppointmentRequest;
import org.shopwave.smartclinic.dto.AppointmentResponse;
import org.shopwave.smartclinic.model.Appointment;
import org.shopwave.smartclinic.model.AppointmentStatus;
import org.shopwave.smartclinic.repository.AppointmentRepository;
import org.shopwave.smartclinic.repository.DoctorRepository;
import org.shopwave.smartclinic.repository.DoctorBlockedSlotRepository;
import org.shopwave.smartclinic.repository.PatientRepository;
import org.shopwave.smartclinic.service.email.EmailService;
import org.shopwave.smartclinic.specification.AppointmentSpecification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
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
    private final DoctorBlockedSlotRepository blockedSlotRepository;
    private final EmailService emailService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              DoctorBlockedSlotRepository blockedSlotRepository,
                              EmailService emailService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.blockedSlotRepository = blockedSlotRepository;
        this.emailService = emailService;
    }

    public List<AppointmentResponse> findByPatient(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(
                        patientId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponse> findByPatientForDoctor(Long patientId, Long doctorId) {
        if (!patientRepository.findById(patientId)
                .map(patient -> doctorId.equals(patient.getAssignedDoctorId()))
                .orElse(false)) {
            throw new IllegalStateException("Only the patient's assigned doctor can view appointments");
        }
        var doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + doctorId));
        return appointmentRepository.findByPatientIdAndDoctorSpecialtyId(
                        patientId, doctor.getSpecialtyId()).stream()
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
        return appointmentRepository.findAll(AppointmentSpecification.byDoctorAndDate(
                        doctorId, date, AppointmentStatus.SCHEDULED)).stream()
                .sorted(java.util.Comparator.comparing(Appointment::getAppointmentTime))
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
        if (!request.getAppointmentDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Appointments must be booked at least one day in advance");
        }
        if (!Boolean.TRUE.equals(pd.doctor.getIsActive())) {
            throw new IllegalStateException("The selected doctor is inactive");
        }
        int duration = pd.doctor.getSlotDurationMinutes();
        LocalTime endTime = request.getAppointmentTime().plusMinutes(duration);
        if (request.getAppointmentTime().isBefore(pd.doctor.getWorkingHoursStart())
                || endTime.isAfter(pd.doctor.getWorkingHoursEnd())) {
            throw new IllegalStateException("Appointment is outside the doctor's working hours");
        }
        long minutesFromStart = Duration.between(
                pd.doctor.getWorkingHoursStart(), request.getAppointmentTime()).toMinutes();
        if (minutesFromStart % duration != 0) {
            throw new IllegalArgumentException("Appointment time is not aligned to the doctor's slots");
        }
        var blocked = blockedSlotRepository.findOverlapping(
                pd.doctor.getId(),
                request.getAppointmentDate().atTime(request.getAppointmentTime()),
                request.getAppointmentDate().atTime(endTime));
        if (!blocked.isEmpty()) {
            throw new IllegalStateException("The selected time is blocked for this doctor");
        }

        // 1. Doctor double-booking: same doctor, same date/time, SCHEDULED
        boolean doctorConflict = appointmentRepository.findOne(AppointmentSpecification.byDoctorAndDateTime(
                pd.doctor.getId(),
                request.getAppointmentDate(),
                request.getAppointmentTime(),
                AppointmentStatus.SCHEDULED)).isPresent();

        if (doctorConflict) {
            throw new IllegalStateException(
                "Doctor " + pd.doctor.getName() + " is already booked at "
                + request.getAppointmentDate() + " " + request.getAppointmentTime()
                + ". Please choose another slot.");
        }

        // 2. Patient double-booking: patient already has a SCHEDULED appointment
        boolean patientConflict = !appointmentRepository.findAll(
                AppointmentSpecification.overlappingAppointments(
                        pd.patient.getId(), request.getAppointmentDate(),
                        request.getAppointmentTime(), endTime)).isEmpty();

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
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();

// After booking, update the patient's assigned doctor so the doctor can see this patient in their list
pd.patient.setAssignedDoctorId(pd.doctor.getId());
patientRepository.save(pd.patient);

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
        if (LocalDateTime.now().isBefore(
                appointment.getAppointmentDate().atTime(appointment.getAppointmentTime()))) {
            throw new IllegalStateException("NO_SHOW can only be recorded after the appointment time");
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
        Appointment saved = appointmentRepository.save(appointment);
        emailService.sendAppointmentCancelled(
                appointment.getPatient().getEmail(),
                appointment.getPatient().getName(),
                appointment.getDoctor().getName(),
                appointment.getAppointmentDate() + " " + appointment.getAppointmentTime());
        return toResponse(saved);
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

        var doctor = request.getDoctorId() != null
                ? doctorRepository.findById(request.getDoctorId())
                    .filter(candidate -> Boolean.TRUE.equals(candidate.getIsActive()))
                    .filter(candidate -> request.getSpecialtyId() == null
                            || request.getSpecialtyId().equals(candidate.getSpecialtyId()))
                    .filter(candidate -> appointmentRepository.countByDoctorIdAndAppointmentDateAndStatus(
                            candidate.getId(), request.getAppointmentDate(), AppointmentStatus.SCHEDULED)
                            < org.shopwave.smartclinic.service.doctor.DoctorService.MAX_DAILY_APPOINTMENTS)
                    .filter(candidate -> isSlotAvailable(candidate, request))
                : request.getSpecialtyId() != null
                ? doctorRepository.findBySpecialtyIdAndIsActiveTrue(request.getSpecialtyId()).stream()
                    .sorted(java.util.Comparator.comparingLong(
                            doctorCandidate -> doctorCandidate.getId() == null
                                    ? Long.MAX_VALUE
                                    : doctorCandidate.getId()))
                    .filter(candidate -> appointmentRepository.countByDoctorIdAndAppointmentDateAndStatus(
                            candidate.getId(), request.getAppointmentDate(), AppointmentStatus.SCHEDULED)
                            < org.shopwave.smartclinic.service.doctor.DoctorService.MAX_DAILY_APPOINTMENTS)
                    .filter(candidate -> isSlotAvailable(candidate, request))
                    .findFirst()
                : patient.getAssignedDoctorId() == null
                    ? java.util.Optional.<org.shopwave.smartclinic.model.Doctor>empty()
                    : doctorRepository.findById(patient.getAssignedDoctorId());
        var selectedDoctor = doctor.orElseThrow(() -> new IllegalStateException(
                request.getDoctorId() != null
                        ? "The selected doctor is unavailable, fully booked, or does not belong to this specialty"
                        : "No doctor in the selected specialty has capacity for that date and time"));

        return new PatientAndDoctor(patient, selectedDoctor);
    }

    private boolean isSlotAvailable(org.shopwave.smartclinic.model.Doctor doctor,
                                    AppointmentRequest request) {
        int duration = doctor.getSlotDurationMinutes();
        LocalTime endTime = request.getAppointmentTime().plusMinutes(duration);
        return !request.getAppointmentTime().isBefore(doctor.getWorkingHoursStart())
                && !endTime.isAfter(doctor.getWorkingHoursEnd())
                && Duration.between(doctor.getWorkingHoursStart(),
                        request.getAppointmentTime()).toMinutes() % duration == 0
                && blockedSlotRepository.findOverlapping(
                        doctor.getId(),
                        request.getAppointmentDate().atTime(request.getAppointmentTime()),
                        request.getAppointmentDate().atTime(endTime)).isEmpty()
                && appointmentRepository.findOne(AppointmentSpecification.byDoctorAndDateTime(
                        doctor.getId(), request.getAppointmentDate(),
                        request.getAppointmentTime(), AppointmentStatus.SCHEDULED)).isEmpty();
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
