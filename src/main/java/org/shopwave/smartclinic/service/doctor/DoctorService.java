package org.shopwave.smartclinic.service.doctor;

import org.shopwave.smartclinic.dto.DoctorRequest;
import org.shopwave.smartclinic.dto.DoctorResponse;
import org.shopwave.smartclinic.model.Doctor;
import org.shopwave.smartclinic.repository.AppointmentRepository;
import org.shopwave.smartclinic.repository.DoctorRepository;
import org.shopwave.smartclinic.repository.DoctorBlockedSlotRepository;
import org.shopwave.smartclinic.repository.SpecialtyRepository;
import org.shopwave.smartclinic.specification.AppointmentSpecification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for {@link Doctor} accounts (Admin-side CRUD plus working-hours mgmt).
 * <p>
 * Doctors are created by Admin only. Working hours and slot duration are
 * managed by the doctor themselves (see {@code user_stories.md} US-DOC-01).
 */
@Service
@Transactional(readOnly = true)
public class DoctorService {

    public static final int MAX_DAILY_APPOINTMENTS = 10;

    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorBlockedSlotRepository blockedSlotRepository;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(DoctorRepository doctorRepository,
                         SpecialtyRepository specialtyRepository,
                         AppointmentRepository appointmentRepository,
                         DoctorBlockedSlotRepository blockedSlotRepository,
                         PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.specialtyRepository = specialtyRepository;
        this.appointmentRepository = appointmentRepository;
        this.blockedSlotRepository = blockedSlotRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<DoctorResponse> findAllActive() {
        return doctorRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<DoctorResponse> findBySpecialty(Long specialtyId) {
        return doctorRepository.findBySpecialtyIdAndIsActiveTrue(specialtyId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public DoctorResponse findById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Doctor not found with id: " + id));
        return toResponse(doctor);
    }

    @Transactional
    public DoctorResponse create(DoctorRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required when creating a doctor");
        }
        if (doctorRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: "
                    + request.getEmail());
        }
        specialtyRepository.findById(request.getSpecialtyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Specialty not found with id: " + request.getSpecialtyId()));

        Doctor doctor = Doctor.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .specialtyId(request.getSpecialtyId())
                .slotDurationMinutes(request.getSlotDurationMinutes())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        return toResponse(doctorRepository.save(doctor));
    }

    @Transactional
    public DoctorResponse update(Long id, DoctorRequest request) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Doctor not found with id: " + id));

        doctorRepository.findByEmail(request.getEmail()).ifPresent(d -> {
            if (!d.getId().equals(id)) {
                throw new IllegalArgumentException("Email already registered: "
                        + request.getEmail());
            }
        });

        doctor.setFirstName(request.getFirstName());
        doctor.setLastName(request.getLastName());
        doctor.setEmail(request.getEmail());
        doctor.setSpecialtyId(request.getSpecialtyId());
        doctor.setSlotDurationMinutes(request.getSlotDurationMinutes());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            doctor.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return toResponse(doctorRepository.save(doctor));
    }

    @Transactional
    public void deactivate(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Doctor not found with id: " + id));
        doctor.setIsActive(false);
        doctorRepository.save(doctor);
    }

    @Transactional
    public void changePassword(Long id, String currentPassword, String newPassword) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Doctor not found with id: " + id));
        if (!passwordEncoder.matches(currentPassword, doctor.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        doctor.setPasswordHash(passwordEncoder.encode(newPassword));
        doctorRepository.save(doctor);
    }

    @Transactional
    public void forceResetPassword(Long id, String newPassword) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Doctor not found with id: " + id));
        doctor.setPasswordHash(passwordEncoder.encode(newPassword));
        doctorRepository.save(doctor);
    }

    @Transactional
    public void setWorkingHours(Long doctorId,
                                java.time.LocalTime start,
                                java.time.LocalTime end,
                                Integer slotDurationMinutes) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Doctor not found with id: " + doctorId));
        if (end.isBefore(start) || end.equals(start)) {
            throw new IllegalArgumentException("Working hours end must be after start");
        }
        if (slotDurationMinutes == null || slotDurationMinutes <= 0) {
            throw new IllegalArgumentException("Slot duration must be positive");
        }
        doctor.setWorkingHoursStart(start);
        doctor.setWorkingHoursEnd(end);
        doctor.setSlotDurationMinutes(slotDurationMinutes);
        doctorRepository.save(doctor);
    }

    /**
     * Returns the available time slots for a doctor on a given date.
     * Slots are generated from the doctor's working hours and slot duration.
     *
     * @param doctorId the doctor's ID
     * @param date     the date to check availability for
     * @return a list of available {@link LocalTime} slots
     */
    public List<LocalTime> getAvailableSlots(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Doctor not found with id: " + doctorId));

        LocalTime start = doctor.getWorkingHoursStart();
        LocalTime end = doctor.getWorkingHoursEnd();
        int slotMinutes = doctor.getSlotDurationMinutes() != null
                ? doctor.getSlotDurationMinutes()
                : 30;

        List<LocalTime> slots = new ArrayList<>();
        LocalTime current = start;
        while (current.isBefore(end)) {
            LocalTime slotEnd = current.plusMinutes(slotMinutes);
            if (!appointmentRepository.findOne(AppointmentSpecification.byDoctorAndDateTime(
                    doctorId, date, current,
                    org.shopwave.smartclinic.model.AppointmentStatus.SCHEDULED)).isPresent()
                    && blockedSlotRepository.findOverlapping(
                        doctorId, date.atTime(current), date.atTime(slotEnd)).isEmpty()) {
                slots.add(current);
            }
            current = current.plusMinutes(slotMinutes);
        }
        return slots;
    }

    public List<LocalTime> getAvailableSlotsBySpecialty(Long specialtyId, LocalDate date) {
        return doctorRepository.findBySpecialtyIdAndIsActiveTrue(specialtyId).stream()
                .filter(doctor -> appointmentRepository.countByDoctorIdAndAppointmentDateAndStatus(
                        doctor.getId(), date, org.shopwave.smartclinic.model.AppointmentStatus.SCHEDULED)
                        < MAX_DAILY_APPOINTMENTS)
                .flatMap(doctor -> getAvailableSlots(doctor.getId(), date).stream())
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Validates a doctor's login credentials and returns a structured response.
     *
     * @param email    the doctor's email address
     * @param password the plain-text password to validate
     * @return a {@link ResponseEntity} containing success/failure details
     */
    public ResponseEntity<Map<String, Object>> validateLoginCredentials(
            String email, String password) {
        Map<String, Object> response = new HashMap<>();
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseGet(() -> null);
        if (doctor == null) {
            response.put("success", false);
            response.put("message", "Invalid email or password");
            return ResponseEntity.badRequest().body(response);
        }
        if (!passwordEncoder.matches(password, doctor.getPasswordHash())) {
            response.put("success", false);
            response.put("message", "Invalid email or password");
            return ResponseEntity.badRequest().body(response);
        }
        if (doctor.getIsActive() == null || !doctor.getIsActive()) {
            response.put("success", false);
            response.put("message", "Doctor account is deactivated");
            return ResponseEntity.badRequest().body(response);
        }
        response.put("success", true);
        response.put("message", "Login successful");
        response.put("doctorId", doctor.getId());
        response.put("email", doctor.getEmail());
        return ResponseEntity.ok(response);
    }

    private DoctorResponse toResponse(Doctor doctor) {
        String specialtyName = doctor.getSpecialty() != null
                ? doctor.getSpecialty().getName()
                : null;
        return DoctorResponse.builder()
                .id(doctor.getId())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .email(doctor.getEmail())
                .specialtyId(doctor.getSpecialtyId())
                .specialtyName(specialtyName)
                .slotDurationMinutes(doctor.getSlotDurationMinutes())
                .workingHoursStart(doctor.getWorkingHoursStart())
                .workingHoursEnd(doctor.getWorkingHoursEnd())
                .active(doctor.getIsActive() != null && doctor.getIsActive())
                .build();
    }
}
