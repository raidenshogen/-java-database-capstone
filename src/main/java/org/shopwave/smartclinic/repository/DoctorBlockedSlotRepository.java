package org.shopwave.smartclinic.repository;

import org.shopwave.smartclinic.model.DoctorBlockedSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DoctorBlockedSlotRepository extends JpaRepository<DoctorBlockedSlot, Long> {

    List<DoctorBlockedSlot> findByDoctorIdOrderByStartDateTimeAsc(Long doctorId);

    @Query("""
        SELECT b FROM DoctorBlockedSlot b
        WHERE b.doctor.id = :doctorId
          AND b.startDateTime < :endDateTime
          AND b.endDateTime > :startDateTime
    """)
    List<DoctorBlockedSlot> findOverlapping(
            @Param("doctorId") Long doctorId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);
}
