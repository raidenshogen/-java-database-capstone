package org.shopwave.smartclinic.specialty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shopwave.smartclinic.dto.SpecialtyRequest;
import org.shopwave.smartclinic.model.Specialty;
import org.shopwave.smartclinic.repository.SpecialtyRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SpecialtyService} — uniqueness enforcement.
 */
@ExtendWith(MockitoExtension.class)
class SpecialtyServiceTest {

    @Mock
    private SpecialtyRepository specialtyRepository;

    @InjectMocks
    private SpecialtyService specialtyService;

    @Test
    void createShouldRejectDuplicateName() {
        SpecialtyRequest request = SpecialtyRequest.builder()
                .name("Cardiology")
                .build();

        when(specialtyRepository.existsByName("Cardiology")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> specialtyService.create(request));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void createShouldSucceedWithUniqueName() {
        SpecialtyRequest request = SpecialtyRequest.builder()
                .name("Dermatology")
                .build();

        when(specialtyRepository.existsByName("Dermatology")).thenReturn(false);
        when(specialtyRepository.save(any(Specialty.class)))
                .thenAnswer(invocation -> {
                    Specialty s = invocation.getArgument(0);
                    s.setId(1L);
                    return s;
                });

        var result = specialtyService.create(request);
        assertNotNull(result.getId());
        assertEquals("Dermatology", result.getName());
    }
}
