package com.clinic.backend.dto.compliance;

import com.clinic.common.enums.EntityType;
import com.clinic.common.enums.ArchivalAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Data Retention Policy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataRetentionPolicyDTO {
    private UUID id;
    private EntityType entityType;
    private Integer retentionDays;
    private Integer gracePeriodDays;
    private ArchivalAction archivalAction;
    private Boolean enabled;
    private Instant lastExecution;
    private Long recordsArchived;
    private Instant createdAt;
    private Instant updatedAt;
}
