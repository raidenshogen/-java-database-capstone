package org.shopwave.smartclinic.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class BlockedSlotResponse {
    Long id;
    Long doctorId;
    LocalDateTime startDateTime;
    LocalDateTime endDateTime;
    String reason;
}
