package org.shopwave.smartclinic.appointment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shopwave.smartclinic.dto.AppointmentRequest;
import org.shopwave.smartclinic.model.Appointment;
import org.shopwave.smartclinic.model.AppointmentStatus;
import org.shopwave.smartclinic.model.Doctor;
import org.shopwave.smartclinic.model.Patient;
import org.shopwave.smartclinic.repository.AppointmentRepository;
import org.shopwave.smartclinic.repository.DoctorRepository;
import org.shopwave.smartclinic.repository.PatientRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AppointmentService} — double-booking prevention.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private static LocalDateTime slot(LocalTime time) {
        return LocalDateTime.of(LocalDate.of(2025, 3, 10), time);
    }

    @Test
    void bookShouldRejectWhenDoctorAlreadyBooked() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setAssignedDoctorId(10L);

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setSlotDurationMinutes(30);

        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(1L)
                .appointmentDate(LocalDate.of(2025, 3, 10))
                .appointmentTime(LocalTime.of(10, 0))
                .build();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findScheduledByDoctorAndDateTime(
                10L, LocalDate.of(2025, 3, 10), LocalTime.of(10, 0), AppointmentStatus.SCHEDULED))
                .thenReturn(Optional.of(new Appointment()));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> appointmentService.book(request));
        assertTrue(ex.getMessage().contains("already booked"));
    }

    @Test
    void bookShouldRejectWhenPatientAlreadyHasAppointment() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setAssignedDoctorId(10L);

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setSlotDurationMinutes(30);

        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(1L)
                .appointmentDate(LocalDate.of(2025, 3, 10))
                .appointmentTime(LocalTime.of(11, 0))
                .build();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findScheduledByDoctorAndDateTime(
                10L, LocalDate.of(2025, 3, 10), LocalTime.of(11, 0), AppointmentStatus.SCHEDULED))
                .thenReturn(Optional.empty());
        when(appointmentRepository.findScheduledByPatient(1L, AppointmentStatus.SCHEDULED))
                .thenReturn(Optional.of(new Appointment()));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> appointmentService.book(request));
        assertTrue(ex.getMessage().contains("already has a scheduled appointment"));
    }

    @Test
    void bookShouldSucceedWhenNoConflicts() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setAssignedDoctorId(10L);

        Doctor doctor = new Doctor();
        doctor.setId(10L);
        doctor.setSlotDurationMinutes(30);

        Appointment saved = new Appointment();
        saved.setId(100L);
        saved.setPatient(patient);
        saved.setDoctor(doctor);

        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(1L)
                .appointmentDate(LocalDate.of(2025, 3, 10))
                .appointmentTime(LocalTime.of(10, 0))
                .build();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findScheduledByDoctorAndDateTime(
                10L, LocalDate.of(2025, 3, 10), LocalTime.of(10, 0), AppointmentStatus.SCHEDULED))
                .thenReturn(Optional.empty());
        when(appointmentRepository.findScheduledByPatient(1L, AppointmentStatus.SCHEDULED))
                .thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(saved);

        var result = appointmentService.book(request);
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(AppointmentStatus.SCHEDULED.name(), result.getStatus());
    }
}
