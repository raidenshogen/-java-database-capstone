package org.shopwave.smartclinic.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SpecialtyStatisticsResponse {
    Long specialtyId;
    String specialtyName;
    long patientCount;
    double percentage;
}
