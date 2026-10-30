package org.shopwave.smartclinic.specialty;

import org.shopwave.smartclinic.dto.SpecialtyRequest;
import org.shopwave.smartclinic.dto.SpecialtyResponse;
import org.shopwave.smartclinic.model.Specialty;
import org.shopwave.smartclinic.repository.SpecialtyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for {@link Specialty} lookup records.
 * <p>
 * Business rule: specialty names must be unique. Admin creates/renames
 * specialties; doctors and patients only read them.
 */
@Service
@Transactional(readOnly = true)
public class SpecialtyService {

    private final SpecialtyRepository specialtyRepository;

    public SpecialtyService(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    public List<SpecialtyResponse> findAll() {
        return specialtyRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public SpecialtyResponse findById(Long id) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Specialty not found with id: " + id));
        return toResponse(specialty);
    }

    @Transactional
    public SpecialtyResponse create(SpecialtyRequest request) {
        if (specialtyRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException(
                    "Specialty already exists: " + request.getName());
        }
        Specialty specialty = Specialty.builder()
                .name(request.getName())
                .build();
        return toResponse(specialtyRepository.save(specialty));
    }

    @Transactional
    public SpecialtyResponse update(Long id, SpecialtyRequest request) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Specialty not found with id: " + id));

        if (!specialty.getName().equals(request.getName())
                && specialtyRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException(
                    "Specialty already exists: " + request.getName());
        }

        specialty.setName(request.getName());
        return toResponse(specialtyRepository.save(specialty));
    }

    @Transactional
    public void delete(Long id) {
        if (!specialtyRepository.existsById(id)) {
            throw new IllegalArgumentException(
                    "Specialty not found with id: " + id);
        }
        specialtyRepository.deleteById(id);
    }

    private SpecialtyResponse toResponse(Specialty specialty) {
        return SpecialtyResponse.builder()
                .id(specialty.getId())
                .name(specialty.getName())
                .build();
    }
}
