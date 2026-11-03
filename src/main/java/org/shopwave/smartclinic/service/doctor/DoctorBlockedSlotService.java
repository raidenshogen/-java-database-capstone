package org.shopwave.smartclinic.service.doctor;

import org.shopwave.smartclinic.dto.BlockedSlotRequest;
import org.shopwave.smartclinic.dto.BlockedSlotResponse;
import org.shopwave.smartclinic.model.Doctor;
import org.shopwave.smartclinic.model.DoctorBlockedSlot;
import org.shopwave.smartclinic.repository.DoctorBlockedSlotRepository;
import org.shopwave.smartclinic.repository.DoctorRepository;
import org.shopwave.smartclinic.repository.AppointmentRepository;
import org.shopwave.smartclinic.model.AppointmentStatus;
import org.shopwave.smartclinic.dto.AppointmentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DoctorBlockedSlotService {
    private final DoctorBlockedSlotRepository blockedSlotRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public DoctorBlockedSlotService(DoctorBlockedSlotRepository blockedSlotRepository,
                                    DoctorRepository doctorRepository,
                                    AppointmentRepository appointmentRepository) {
        this.blockedSlotRepository = blockedSlotRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public List<AppointmentResponse> findConflicts(Long doctorId, LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusOrderByAppointmentTime(
                        doctorId, start.toLocalDate(), AppointmentStatus.SCHEDULED).stream()
                .filter(a -> a.getAppointmentTime().isBefore(end.toLocalTime())
                        && a.getAppointmentTime().plusMinutes(a.getDoctor().getSlotDurationMinutes())
                        .isAfter(start.toLocalTime()))
                .map(a -> AppointmentResponse.builder()
                        .id(a.getId()).patientId(a.getPatient().getId())
                        .patientName(a.getPatient().getName()).doctorId(doctorId)
                        .doctorName(a.getDoctor().getName())
                        .appointmentDate(a.getAppointmentDate())
                        .appointmentTime(a.getAppointmentTime())
                        .status(a.getStatus().name()).build())
                .toList();
    }

    @Transactional
    public BlockedSlotResponse create(Long doctorId, BlockedSlotRequest request) {
        if (request.getEndDateTime() == null || request.getStartDateTime() == null
                || !request.getEndDateTime().isAfter(request.getStartDateTime())) {
            throw new IllegalArgumentException("Blocked slot end must be after start");
        }
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + doctorId));
        DoctorBlockedSlot slot = DoctorBlockedSlot.builder()
                .doctor(doctor)
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .reason(request.getReason())
                .build();
        return toResponse(blockedSlotRepository.save(slot));
    }

    public List<BlockedSlotResponse> findOverlapping(Long doctorId, LocalDateTime start, LocalDateTime end) {
        return blockedSlotRepository.findOverlapping(doctorId, start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BlockedSlotResponse> findByDoctor(Long doctorId) {
        return blockedSlotRepository.findByDoctorIdOrderByStartDateTimeAsc(doctorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long doctorId, Long slotId) {
        DoctorBlockedSlot slot = blockedSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Blocked slot not found with id: " + slotId));
        if (!slot.getDoctor().getId().equals(doctorId)) {
            throw new IllegalStateException("Blocked slot does not belong to this doctor");
        }
        blockedSlotRepository.delete(slot);
    }

    private BlockedSlotResponse toResponse(DoctorBlockedSlot slot) {
        return BlockedSlotResponse.builder()
                .id(slot.getId())
                .doctorId(slot.getDoctor().getId())
                .startDateTime(slot.getStartDateTime())
                .endDateTime(slot.getEndDateTime())
                .reason(slot.getReason())
                .build();
    }
}
