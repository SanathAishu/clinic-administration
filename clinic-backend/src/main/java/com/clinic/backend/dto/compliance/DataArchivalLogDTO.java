package com.clinic.backend.dto.compliance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for Data Archival Execution Log
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataArchivalLogDTO {
    private UUID id;
    private UUID policyId;
    private LocalDate executionDate;
    private String entityType;
    private Long recordsProcessed;
    private Long recordsArchived;
    private Long recordsFailed;
    private Instant startTime;
    private Instant endTime;
    private Integer durationSeconds;
    private String status; // RUNNING, COMPLETED, FAILED
    private String errorMessage;
    private Instant createdAt;
}
