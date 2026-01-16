package com.clinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for current queue status display
 * Used by digital display boards in clinic waiting areas
 *
 * Information included:
 * - Current token being served
 * - Next token in queue
 * - Number of patients waiting
 * - Average wait time estimate
 * - Queue utilization
 * - Stability status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueueStatusDTO {

    /**
     * Current token number being served
     * Null if no appointment in progress
     */
    private Integer currentToken;

    /**
     * Next token number to be called
     * Null if queue is empty
     */
    private Integer nextToken;

    /**
     * Number of patients currently waiting
     */
    private Integer patientsWaiting;

    /**
     * Average estimated wait time in minutes
     * Based on M/M/1 formula: W = 1/(μ - λ)
     */
    private Integer avgWaitTimeMinutes;

    /**
     * Queue utilization (ρ = λ/μ)
     * Range: [0, 1)
     * Higher values indicate busier queue
     */
    private Double utilization;

    /**
     * Whether queue is stable (λ < μ)
     * False indicates potential for excessive wait times
     */
    private Boolean isStable;

    /**
     * Timestamp when this status was captured
     */
    private Instant timestamp;
}
