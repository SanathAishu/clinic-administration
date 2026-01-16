package com.clinic.backend.controller;

import com.clinic.backend.dto.DispensePrescriptionRequest;
import com.clinic.backend.dto.PrescriptionDispensingResponse;
import com.clinic.backend.service.DrugInteractionService.DrugInteractionException;
import com.clinic.backend.service.PrescriptionService;
import com.clinic.backend.service.PrescriptionService.InsufficientStockException;
import com.clinic.common.entity.clinical.Prescription;
import com.clinic.common.entity.clinical.PrescriptionItem;
import com.clinic.common.entity.core.User;
import com.clinic.common.entity.operational.Inventory;
import com.clinic.common.enums.PrescriptionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API Controller for enhanced prescription operations
 *
 * Provides endpoints for:
 * - Dispensing prescriptions with atomic inventory integration
 * - Completing prescriptions
 * - Cancelling prescriptions
 * - Refilling prescriptions
 * - Checking drug interactions
 *
 * All endpoints support multi-tenancy with tenant isolation.
 *
 * Reference: Phase D Feature 3 - Prescription Enhancement
 * Mathematical Foundation: Theorem 12 - Atomic Inventory Deduction
 */
@Slf4j
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionEnhancedController {

    private final PrescriptionService prescriptionService;

    /**
     * Dispense prescription with atomic inventory deduction.
     *
     * POST /api/prescriptions/{id}/dispense
     *
     * Theorem 12: Atomic Inventory Deduction
     * Either both prescription is marked DISPENSED AND inventory is reduced,
     * or neither happens (atomic transaction with rollback on failure).
     *
     * Process:
     * 1. Validate prescription exists and status is PENDING
     * 2. Check all items have sufficient inventory (fail-fast)
     * 3. Check for drug interactions
     * 4. Record inventory transactions (SALE type)
     * 5. Mark prescription DISPENSED
     * 6. Return detailed response with inventory changes
     *
     * @param prescriptionId Prescription to dispense
     * @param tenantId Tenant context
     * @param request Request with dispensedBy user ID
     * @return Response with dispensing details
     * @throws InsufficientStockException if inventory insufficient
     * @throws DrugInteractionException if SEVERE interaction
     * @throws IllegalStateException if invalid state transition
     */
    @PostMapping("/{id}/dispense")
    public ResponseEntity<PrescriptionDispensingResponse> dispensePrescription(
            @PathVariable("id") UUID prescriptionId,
            @RequestParam UUID tenantId,
            @RequestBody DispensePrescriptionRequest request) {

        log.info("Dispensing prescription {} by user {}", prescriptionId, request.getDispensedBy());

        try {
            // Fetch dispensed by user (in real implementation, would query User repository)
            // For now, we create a stub User object with the ID
            User dispensedBy = new User();
            dispensedBy.setId(request.getDispensedBy());

            // Get prescription before dispensing
            Prescription prescription = prescriptionService.getPrescriptionById(prescriptionId, tenantId);
            PrescriptionDispensingResponse.DispensedItemDetail[] itemsBeforeDispensing =
                captureItemDetails(prescription);

            // Dispense prescription (atomic operation with inventory deduction)
            Prescription dispensed = prescriptionService.dispensePrescription(
                prescriptionId,
                tenantId,
                dispensedBy
            );

            // Build successful response
            return ResponseEntity.ok(buildDispensingResponse(dispensed, itemsBeforeDispensing, true, null));

        } catch (InsufficientStockException e) {
            log.error("Insufficient stock for prescription {}: {}", prescriptionId, e.getMessage());
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildErrorResponse(prescriptionId, "Insufficient Stock", e.getMessage()));

        } catch (DrugInteractionException e) {
            log.error("Drug interaction detected for prescription {}: {}", prescriptionId, e.getMessage());
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildErrorResponse(prescriptionId, "Drug Interaction", e.getMessage()));

        } catch (IllegalStateException e) {
            log.error("Invalid state transition for prescription {}: {}", prescriptionId, e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(prescriptionId, "Invalid State", e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error dispensing prescription {}", prescriptionId, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(prescriptionId, "Dispensing Error", e.getMessage()));
        }
    }

    /**
     * Complete prescription.
     *
     * POST /api/prescriptions/{id}/complete
     *
     * Marks prescription as COMPLETED (patient finished medication).
     * Only allowed from DISPENSED state.
     *
     * @param prescriptionId Prescription to complete
     * @param tenantId Tenant context
     * @return Updated prescription
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completePrescription(
            @PathVariable("id") UUID prescriptionId,
            @RequestParam UUID tenantId) {

        log.info("Completing prescription {}", prescriptionId);

        try {
            Prescription completed = prescriptionService.completePrescription(prescriptionId, tenantId);
            return ResponseEntity.ok(completed);

        } catch (IllegalStateException e) {
            log.error("Cannot complete prescription {}: {}", prescriptionId, e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Invalid State", e.getMessage()));

        } catch (Exception e) {
            log.error("Error completing prescription {}", prescriptionId, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Completion Error", e.getMessage()));
        }
    }

    /**
     * Cancel prescription.
     *
     * POST /api/prescriptions/{id}/cancel
     *
     * Cancels prescription. Can be called from PENDING or DISPENSED states.
     * No inventory recovery on cancellation (already dispensed or other reasons).
     *
     * @param prescriptionId Prescription to cancel
     * @param tenantId Tenant context
     * @param cancelledBy User cancelling the prescription
     * @return Updated prescription
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelPrescription(
            @PathVariable("id") UUID prescriptionId,
            @RequestParam UUID tenantId,
            @RequestParam UUID cancelledBy) {

        log.info("Cancelling prescription {} by user {}", prescriptionId, cancelledBy);

        try {
            User cancelledByUser = new User();
            cancelledByUser.setId(cancelledBy);
            Prescription cancelled = prescriptionService.cancelPrescription(
                prescriptionId,
                tenantId,
                cancelledByUser
            );
            return ResponseEntity.ok(cancelled);

        } catch (IllegalStateException e) {
            log.error("Cannot cancel prescription {}: {}", prescriptionId, e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Invalid State", e.getMessage()));

        } catch (Exception e) {
            log.error("Error cancelling prescription {}", prescriptionId, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Cancellation Error", e.getMessage()));
        }
    }

    /**
     * Refill prescription.
     *
     * POST /api/prescriptions/{id}/refill
     *
     * Creates a new PENDING prescription as a copy of the original.
     * Only allowed if original is COMPLETED and refill limit not exceeded.
     *
     * Theorem 11: Quantity Invariant
     * Refill only allowed if: timesFilled < (allowedRefills + 1)
     *
     * @param prescriptionId Original prescription to refill
     * @param tenantId Tenant context
     * @param refillRequestedBy User requesting refill
     * @return New prescription in PENDING state
     */
    @PostMapping("/{id}/refill")
    public ResponseEntity<?> refillPrescription(
            @PathVariable("id") UUID prescriptionId,
            @RequestParam UUID tenantId,
            @RequestParam UUID refillRequestedBy) {

        log.info("Creating refill for prescription {} by user {}", prescriptionId, refillRequestedBy);

        try {
            User refillBy = new User();
            refillBy.setId(refillRequestedBy);
            Prescription refilled = prescriptionService.refillPrescription(
                prescriptionId,
                tenantId,
                refillBy
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(refilled);

        } catch (IllegalStateException e) {
            log.error("Cannot refill prescription {}: {}", prescriptionId, e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Cannot Refill", e.getMessage()));

        } catch (Exception e) {
            log.error("Error refilling prescription {}", prescriptionId, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Refill Error", e.getMessage()));
        }
    }

    /**
     * Get prescription with full details.
     *
     * GET /api/prescriptions/{id}
     *
     * @param prescriptionId Prescription ID
     * @param tenantId Tenant context
     * @return Prescription details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPrescription(
            @PathVariable("id") UUID prescriptionId,
            @RequestParam UUID tenantId) {

        try {
            Prescription prescription = prescriptionService.getPrescriptionById(prescriptionId, tenantId);
            return ResponseEntity.ok(prescription);

        } catch (IllegalArgumentException e) {
            log.error("Prescription not found: {}", prescriptionId);
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Not Found", "Prescription not found"));

        } catch (Exception e) {
            log.error("Error fetching prescription {}", prescriptionId, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Fetch Error", e.getMessage()));
        }
    }

    /**
     * Get prescriptions by status.
     *
     * GET /api/prescriptions/status/{status}
     *
     * @param status Prescription status filter
     * @param tenantId Tenant context
     * @return List of prescriptions with given status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getPrescriptionsByStatus(
            @PathVariable("status") PrescriptionStatus status,
            @RequestParam UUID tenantId) {

        try {
            List<Prescription> prescriptions = prescriptionService.getPrescriptionsByStatus(tenantId, status);
            return ResponseEntity.ok(prescriptions);

        } catch (Exception e) {
            log.error("Error fetching prescriptions by status {}", status, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Fetch Error", e.getMessage()));
        }
    }

    // Helper methods

    /**
     * Capture item details before dispensing for response
     */
    private PrescriptionDispensingResponse.DispensedItemDetail[] captureItemDetails(Prescription prescription) {
        return prescription.getItems().stream()
            .map(item -> PrescriptionDispensingResponse.DispensedItemDetail.builder()
                .medicationName(item.getInventory().getItemName())
                .medicationCode(item.getInventory().getItemCode())
                .prescribedQuantity(item.getPrescribedQuantity())
                .dispensedQuantity(item.getDispensedQuantity())
                .dosageUnit(item.getDosageUnit())
                .dosage(item.getDosage())
                .frequencyPerDay(item.getFrequencyPerDay())
                .durationDays(item.getDurationDays())
                .instructions(item.getInstructions())
                .stockBefore(item.getInventory().getCurrentStock())
                .build())
            .toArray(PrescriptionDispensingResponse.DispensedItemDetail[]::new);
    }

    /**
     * Build successful dispensing response
     */
    private PrescriptionDispensingResponse buildDispensingResponse(
            Prescription prescription,
            PrescriptionDispensingResponse.DispensedItemDetail[] itemDetails,
            Boolean success,
            String errorMessage) {

        // Update item details with final stock after
        List<PrescriptionDispensingResponse.DispensedItemDetail> items = new ArrayList<>();
        for (int i = 0; i < prescription.getItems().size(); i++) {
            PrescriptionItem item = prescription.getItems().get(i);
            if (i < itemDetails.length) {
                itemDetails[i].setDispensedQuantity(item.getDispensedQuantity());
                itemDetails[i].setStockAfter(item.getInventory().getCurrentStock());
                items.add(itemDetails[i]);
            }
        }

        return PrescriptionDispensingResponse.builder()
            .prescriptionId(prescription.getId())
            .status(prescription.getStatus())
            .dispensedAt(prescription.getDispensedAt())
            .dispensedBy(prescription.getDispensedBy().getId().toString())
            .patientName(prescription.getPatient().getFirstName() + " " + prescription.getPatient().getLastName())
            .patientId(prescription.getPatient().getId())
            .doctorName(prescription.getDoctor().getFirstName() + " " + prescription.getDoctor().getLastName())
            .doctorId(prescription.getDoctor().getId())
            .totalItemsDispensed(prescription.getItems().size())
            .itemsDispensed(items)
            .inventoryTransactionsCreated(prescription.getItems().size())
            .interactions(new ArrayList<>())
            .success(success)
            .errorMessage(errorMessage)
            .build();
    }

    /**
     * Build error response for failed dispensing
     */
    private PrescriptionDispensingResponse buildErrorResponse(
            UUID prescriptionId,
            String errorType,
            String errorMessage) {

        return PrescriptionDispensingResponse.builder()
            .prescriptionId(prescriptionId)
            .success(false)
            .errorMessage(String.format("%s: %s", errorType, errorMessage))
            .build();
    }

    /**
     * Simple error response DTO
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    static class ErrorResponse {
        private String error;
        private String message;
    }
}
