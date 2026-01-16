package com.clinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.UUID;

/**
 * DTO for estimated wait time for a specific appointment
 *
 * Contains:
 * - Estimated wait time in minutes
 * - Confidence level
 * - Queue stability indicator
 * - Underlying M/M/1 parameters for analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WaitTimeDTO {

    /**
     * Appointment ID this estimate is for
     */
    private UUID appointmentId;

    /**
     * Estimated wait time in minutes
     * Based on M/M/1 formula adjusted for queue position:
     * W_adjusted = W * ceil(position / 5)
     * This provides conservative estimates
     */
    private Integer estimatedWaitMinutes;

    /**
     * Confidence level in the estimate
     * Values: "LOW", "MEDIUM", "HIGH"
     * - LOW: Queue is unstable or minimal historical data
     * - MEDIUM: Normal queue conditions
     * - HIGH: Stable queue with good historical data
     */
    private String confidence;

    /**
     * Whether the queue is currently unstable (λ ≥ μ)
     * True indicates potential for longer-than-estimated wait times
     */
    private Boolean isUnstable;

    /**
     * Queue utilization at time of calculation
     * ρ = λ/μ, range [0, 1)
     * Higher values indicate longer potential wait times
     */
    private Double utilization;

    /**
     * Arrival rate used in calculation
     * λ = patients per hour
     */
    private Double arrivalRate;

    /**
     * Service rate used in calculation
     * μ = patients per hour
     */
    private Double serviceRate;
}
