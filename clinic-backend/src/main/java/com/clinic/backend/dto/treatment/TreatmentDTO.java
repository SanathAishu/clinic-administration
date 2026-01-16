package com.clinic.backend.dto.treatment;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentDTO {

    private UUID id;

    private String name;

    private String description;

    private String category;

    private BigDecimal baseCost;

    private BigDecimal discountPercentage;

    private BigDecimal finalCost;

    private BigDecimal discountAmount;

    private Integer durationMinutes;

    private String instructions;

    private String prerequisites;

    private UUID branchId;

    private Boolean isActive;

    private UUID createdById;

    private String createdByName;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant deletedAt;
}
