package com.clinic.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.UUID;

/**
 * DTO for patient's current position in the queue
 *
 * Contains:
 * - Current position (1-indexed)
 * - Total queue length
 * - Number of patients ahead
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueuePositionDTO {

    /**
     * Appointment ID
     */
    private UUID appointmentId;

    /**
     * Current position in queue (1-indexed)
     * Position = 1 means next to be served
     */
    private Integer position;

    /**
     * Total number of appointments in queue
     * Includes the appointment itself
     */
    private Integer queueLength;

    /**
     * Number of patients ahead in queue
     * aheadCount = position - 1
     */
    private Integer aheadCount;
}
