package com.clinic.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for dispensing prescription request
 *
 * Contains the minimal information needed to dispense a prescription:
 * - prescriptionId: which prescription to dispense
 * - dispensedBy: which user is performing the dispensing (for audit trail)
 *
 * Response will be a PrescriptionDispensingResponse with detailed dispensing result.
 *
 * Example:
 * {
 *   "prescriptionId": "550e8400-e29b-41d4-a716-446655440000",
 *   "dispensedBy": "550e8400-e29b-41d4-a716-446655440001"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispensePrescriptionRequest {

    /**
     * ID of prescription to dispense
     */
    @NotNull(message = "Prescription ID is required")
    private UUID prescriptionId;

    /**
     * ID of user performing dispensing (pharmacy staff)
     * Used for audit trail to track who dispensed the prescription
     */
    @NotNull(message = "Dispensed by user ID is required")
    private UUID dispensedBy;
}
