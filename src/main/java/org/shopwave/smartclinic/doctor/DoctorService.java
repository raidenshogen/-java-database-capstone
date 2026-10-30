package org.shopwave.smartclinic.doctor;

import org.shopwave.smartclinic.dto.DoctorRequest;
import org.shopwave.smartclinic.dto.DoctorResponse;
import org.shopwave.smartclinic.model.Doctor;
import org.shopwave.smartclinic.repository.AppointmentRepository;
import org.shopwave.smartclinic.repository.DoctorRepository;
import org.shopwave.smartclinic.repository.SpecialtyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(DoctorRepository doctorRepository,
                         SpecialtyRepository specialtyRepository,
                         AppointmentRepository appointmentRepository,
                         PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.specialtyRepository = specialtyRepository;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<DoctorResponse> findAllActive() {
        return doctorRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<DoctorResponse> findBySpecialty(Long specialtyId) {
        return doctorRepository.findBySpecialtyIdAndIsActiveTrue(
                        specialtyId.intValue()).stream()
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
                .specialtyId(request.getSpecialtyId().intValue())
                .slotDurationMinutes(request.getSlotDurationMinutes())
                .isActive(true)
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
        doctor.setSpecialtyId(request.getSpecialtyId().intValue());
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
            slots.add(current);
            current = current.plusMinutes(slotMinutes);
        }
        return slots;
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
                .specialtyName(specialtyName)
                .slotDurationMinutes(doctor.getSlotDurationMinutes())
                .active(doctor.getIsActive() != null && doctor.getIsActive())
                .build();
    }
}
