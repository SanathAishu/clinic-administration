package com.clinic.backend.service;

import com.clinic.common.entity.clinical.Prescription;
import com.clinic.common.entity.clinical.PrescriptionItem;
import com.clinic.common.entity.core.User;
import com.clinic.common.entity.operational.Inventory;
import com.clinic.common.entity.operational.InventoryTransaction;
import com.clinic.backend.repository.PrescriptionRepository;
import com.clinic.backend.service.DrugInteractionService.DrugInteractionException;
import com.clinic.common.enums.PrescriptionStatus;
import com.clinic.common.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Prescription Service - Manages prescription lifecycle and dispensing
 *
 * Critical responsibility: dispensePrescription() method implements atomic
 * inventory deduction with guaranteed ACID properties.
 *
 * Theorem 12: Atomic Inventory Deduction
 * When dispensing prescription, inventory reduction is atomic - either both
 * prescription is marked DISPENSED and inventory is reduced, or neither happens.
 *
 * Reference: Phase D Feature 3 - Prescription Enhancement
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final InventoryService inventoryService;
    private final InventoryTransactionService inventoryTransactionService;
    private final DrugInteractionService drugInteractionService;

    /**
     * Create new prescription
     *
     * @param prescription Prescription to create
     * @param tenantId Tenant context
     * @param createdBy User creating the prescription
     * @return Created prescription
     */
    @Transactional
    public Prescription createPrescription(Prescription prescription, UUID tenantId, User createdBy) {
        log.debug("Creating prescription for patient: {}", prescription.getPatient().getId());

        prescription.setTenantId(tenantId);
        prescription.setCreatedBy(createdBy);

        if (prescription.getPrescriptionDate() == null) {
            prescription.setPrescriptionDate(LocalDate.now());
        }

        if (prescription.getStatus() == null) {
            prescription.setStatus(PrescriptionStatus.PENDING);
        }

        if (prescription.getAllowedRefills() == null) {
            prescription.setAllowedRefills(0);
        }

        if (prescription.getTimesFilled() == null) {
            prescription.setTimesFilled(0);
        }

        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Created prescription: {} for patient: {} with {} items",
            saved.getId(), saved.getPatient().getId(), saved.getItems().size());
        return saved;
    }

    /**
     * Get prescription by ID with tenant isolation
     *
     * @param id Prescription ID
     * @param tenantId Tenant context
     * @return Prescription
     * @throws IllegalArgumentException if not found
     */
    public Prescription getPrescriptionById(UUID id, UUID tenantId) {
        return prescriptionRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found: " + id));
    }

    /**
     * Get prescriptions for patient with pagination
     *
     * @param patientId Patient ID
     * @param tenantId Tenant context
     * @param pageable Pagination parameters
     * @return Paginated prescriptions
     */
    public Page<Prescription> getPrescriptionsForPatient(UUID patientId, UUID tenantId, Pageable pageable) {
        return prescriptionRepository.findByPatientIdAndTenantIdAndDeletedAtIsNull(patientId, tenantId, pageable);
    }

    /**
     * Get prescriptions by doctor
     *
     * @param doctorId Doctor ID
     * @param tenantId Tenant context
     * @return List of prescriptions
     */
    public List<Prescription> getPrescriptionsByDoctor(UUID doctorId, UUID tenantId) {
        return prescriptionRepository.findByDoctorIdAndTenantId(doctorId, tenantId);
    }

    /**
     * Get active (PENDING) prescriptions for patient
     *
     * @param patientId Patient ID
     * @param tenantId Tenant context
     * @return List of active prescriptions
     */
    public List<Prescription> getActivePrescriptionsForPatient(UUID patientId, UUID tenantId) {
        return prescriptionRepository.findActivePrescriptionsForPatient(patientId, tenantId);
    }

    /**
     * Get prescriptions by status
     *
     * @param tenantId Tenant context
     * @param status Prescription status filter
     * @return List of prescriptions with given status
     */
    public List<Prescription> getPrescriptionsByStatus(UUID tenantId, PrescriptionStatus status) {
        return prescriptionRepository.findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status);
    }

    /**
     * CRITICAL METHOD: Dispense prescription with atomic inventory deduction.
     *
     * Theorem 12: Atomic Inventory Deduction
     * Either both prescription is marked DISPENSED AND inventory is reduced,
     * or neither happens. @Transactional guarantees atomicity via database rollback.
     *
     * Algorithm:
     * 1. Fetch prescription in PENDING state
     * 2. Validate refill limit not exceeded
     * 3. FOR EACH item, check inventory sufficient (fail-fast)
     * 4. Check drug interactions (fail-fast)
     * 5. FOR EACH item, record inventory transaction (SALE type)
     * 6. Mark prescription DISPENSED with timestamp
     * 7. Persist (commit if all success, rollback if any failure)
     *
     * Atomicity guarantee:
     * - If any step fails → @Transactional rolls back ALL changes
     * - Prescription NOT marked DISPENSED ↔ Inventory NOT reduced
     * - Result: Either both succeed or both fail with no partial state
     *
     * @param prescriptionId Prescription to dispense
     * @param tenantId Tenant context
     * @param dispensedBy User performing dispensing
     * @return Updated prescription (status DISPENSED)
     *
     * @throws IllegalArgumentException if prescription not found
     * @throws IllegalStateException if status not PENDING or refill limit exceeded
     * @throws InsufficientStockException if inventory insufficient for any item
     * @throws DrugInteractionException if SEVERE interaction detected
     */
    @Transactional
    public Prescription dispensePrescription(
            UUID prescriptionId,
            UUID tenantId,
            User dispensedBy) {

        // Step 1: Fetch prescription and validate state
        log.debug("Starting dispensing for prescription: {}", prescriptionId);
        Prescription prescription = getPrescriptionById(prescriptionId, tenantId);

        // Validate state transition (DAG)
        if (prescription.getStatus() != PrescriptionStatus.PENDING) {
            String error = String.format(
                "Cannot dispense prescription in status: %s (expected PENDING)",
                prescription.getStatus()
            );
            log.error(error);
            throw new IllegalStateException(error);
        }

        // Step 2: Validate refill limit
        if (prescription.getTimesFilled() >= (prescription.getAllowedRefills() + 1)) {
            String error = String.format(
                "Prescription refill limit exceeded: %d / %d fills",
                prescription.getTimesFilled(),
                prescription.getAllowedRefills() + 1
            );
            log.error(error);
            throw new IllegalStateException(error);
        }

        List<PrescriptionItem> items = prescription.getItems();

        // Step 3: Check ALL items have sufficient inventory (fail-fast validation)
        log.debug("Validating inventory for {} items", items.size());
        for (PrescriptionItem item : items) {
            Inventory inventory = inventoryService.getInventoryItemById(
                item.getInventory().getId(), tenantId
            );

            if (inventory.getCurrentStock() < item.getPrescribedQuantity()) {
                String error = String.format(
                    "Insufficient stock for %s: need %d, have %d",
                    inventory.getItemName(),
                    item.getPrescribedQuantity(),
                    inventory.getCurrentStock()
                );
                log.error(error);
                throw new InsufficientStockException(error);
            }
        }

        // Step 4: Check drug interactions (fail-fast)
        log.debug("Checking drug interactions");
        try {
            drugInteractionService.checkDrugInteractions(items, prescription.getPatient());
        } catch (DrugInteractionException e) {
            log.error("Drug interaction check failed: {}", e.getMessage());
            throw e;
        }

        // Step 5: Deduct inventory for each item within transaction
        // This is where atomicity is critical
        log.debug("Recording inventory transactions for {} items", items.size());
        for (PrescriptionItem item : items) {
            Inventory inventory = inventoryService.getInventoryItemById(
                item.getInventory().getId(), tenantId
            );

            // Create inventory transaction (SALE type)
            InventoryTransaction transaction = InventoryTransaction.builder()
                .inventory(inventory)
                .transactionType(TransactionType.SALE)
                .quantity(item.getPrescribedQuantity())
                .unitPrice(inventory.getUnitPrice())
                .referenceType("PRESCRIPTION")
                .referenceId(prescriptionId)
                .notes(String.format("Prescription dispensed: %s", prescriptionId))
                .createdBy(dispensedBy)
                .build();
            transaction.setTenantId(tenantId);

            log.debug("Recording transaction for inventory {}: {} units",
                inventory.getId(), item.getPrescribedQuantity());
            inventoryTransactionService.createTransaction(transaction, tenantId);

            // Update dispensed quantity on item
            item.setDispensedQuantity(item.getPrescribedQuantity());
        }

        // Step 6: Mark prescription as DISPENSED
        log.debug("Marking prescription {} as DISPENSED", prescriptionId);
        prescription.markAsDispensed(dispensedBy);

        // Step 7: Persist and commit transaction
        // If any step 3-6 failed → @Transactional rolls back ALL changes
        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Successfully dispensed prescription {}: {} items, {} units",
            saved.getId(), saved.getItems().size(),
            saved.getItems().stream()
                .mapToInt(PrescriptionItem::getDispensedQuantity)
                .sum());

        return saved;

        // ACID Guarantee:
        // Either:
        // A) All changes committed: prescription DISPENSED + inventory reduced + transactions recorded
        // B) All changes rolled back: prescription PENDING + inventory unchanged + no transactions
        // Result: No partial state, atomicity maintained ✓
    }

    /**
     * Complete prescription (mark as COMPLETED).
     *
     * Typically called when patient finishes taking the medication.
     *
     * @param prescriptionId Prescription to complete
     * @param tenantId Tenant context
     * @return Updated prescription (status COMPLETED)
     * @throws IllegalStateException if status not DISPENSED
     */
    @Transactional
    public Prescription completePrescription(UUID prescriptionId, UUID tenantId) {
        log.debug("Completing prescription: {}", prescriptionId);
        Prescription prescription = getPrescriptionById(prescriptionId, tenantId);

        prescription.markAsCompleted();
        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Completed prescription: {}", saved.getId());
        return saved;
    }

    /**
     * Cancel prescription.
     *
     * Can be called from PENDING or DISPENSED states.
     * No inventory recovery on cancellation.
     *
     * @param prescriptionId Prescription to cancel
     * @param tenantId Tenant context
     * @param cancelledBy User performing cancellation
     * @return Updated prescription (status CANCELLED)
     * @throws IllegalStateException if status is terminal
     */
    @Transactional
    public Prescription cancelPrescription(
            UUID prescriptionId,
            UUID tenantId,
            User cancelledBy) {
        log.debug("Cancelling prescription: {}", prescriptionId);
        Prescription prescription = getPrescriptionById(prescriptionId, tenantId);

        prescription.cancel(cancelledBy);
        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Cancelled prescription: {}", saved.getId());
        return saved;
    }

    /**
     * Refill prescription (create new PENDING prescription from completed one).
     *
     * Theorem 11: Quantity Invariant
     * Refill only allowed if:
     * 1. Current status is COMPLETED
     * 2. timesFilled < (allowedRefills + 1)
     *
     * Creates a new prescription with same items, doctor, and patient.
     * The new prescription starts in PENDING state and can be dispensed again.
     *
     * @param prescriptionId Original prescription to refill
     * @param tenantId Tenant context
     * @param refillRequestedBy User requesting refill
     * @return New prescription in PENDING state
     * @throws IllegalStateException if prescription cannot be refilled
     */
    @Transactional
    public Prescription refillPrescription(
            UUID prescriptionId,
            UUID tenantId,
            User refillRequestedBy) {
        log.debug("Creating refill for prescription: {}", prescriptionId);
        Prescription original = getPrescriptionById(prescriptionId, tenantId);

        if (!original.canBeRefilled()) {
            String error = String.format(
                "Prescription cannot be refilled: status=%s, fills=%d/%d",
                original.getStatus(),
                original.getTimesFilled(),
                original.getAllowedRefills() + 1
            );
            log.error(error);
            throw new IllegalStateException(error);
        }

        // Create new prescription as copy
        Prescription newRx = Prescription.builder()
            .patient(original.getPatient())
            .doctor(original.getDoctor())
            .medicalRecord(original.getMedicalRecord())
            .prescriptionDate(LocalDate.now())
            .status(PrescriptionStatus.PENDING)
            .notes(String.format("Refill of prescription %s", original.getId()))
            .items(new ArrayList<>())
            .allowedRefills(original.getAllowedRefills())
            .timesFilled(0)  // New prescription starts at 0
            .createdBy(refillRequestedBy)
            .build();
        newRx.setTenantId(tenantId);

        // Deep copy items
        for (PrescriptionItem item : original.getItems()) {
            PrescriptionItem newItem = PrescriptionItem.builder()
                .prescription(newRx)
                .inventory(item.getInventory())
                .dosage(item.getDosage())
                .dosageUnit(item.getDosageUnit())
                .frequencyPerDay(item.getFrequencyPerDay())
                .durationDays(item.getDurationDays())
                .prescribedQuantity(item.getPrescribedQuantity())
                .dispensedQuantity(0)
                .instructions(item.getInstructions())
                .build();
            newItem.setTenantId(tenantId);
            newRx.getItems().add(newItem);
        }

        Prescription saved = prescriptionRepository.save(newRx);
        log.info("Created refill prescription {} from original {}", saved.getId(), original.getId());
        return saved;
    }

    /**
     * Update prescription (only allowed in PENDING state)
     *
     * @param prescriptionId Prescription to update
     * @param tenantId Tenant context
     * @param updates Update data
     * @return Updated prescription
     * @throws IllegalStateException if prescription is completed or cancelled
     */
    @Transactional
    public Prescription updatePrescription(UUID prescriptionId, UUID tenantId, Prescription updates) {
        log.debug("Updating prescription: {}", prescriptionId);
        Prescription prescription = getPrescriptionById(prescriptionId, tenantId);

        // Only allow updates if in PENDING state
        if (prescription.getStatus() != PrescriptionStatus.PENDING) {
            String error = String.format(
                "Cannot update prescription in status: %s (only PENDING allowed)",
                prescription.getStatus()
            );
            log.error(error);
            throw new IllegalStateException(error);
        }

        if (updates.getNotes() != null) prescription.setNotes(updates.getNotes());
        if (updates.getAllowedRefills() != null) prescription.setAllowedRefills(updates.getAllowedRefills());

        return prescriptionRepository.save(prescription);
    }

    /**
     * Soft delete prescription
     *
     * @param prescriptionId Prescription to delete
     * @param tenantId Tenant context
     */
    @Transactional
    public void softDeletePrescription(UUID prescriptionId, UUID tenantId) {
        log.debug("Soft deleting prescription: {}", prescriptionId);
        Prescription prescription = getPrescriptionById(prescriptionId, tenantId);
        prescription.softDelete();
        prescriptionRepository.save(prescription);
        log.info("Soft deleted prescription: {}", prescriptionId);
    }

    /**
     * Count prescriptions for patient
     *
     * @param patientId Patient ID
     * @param tenantId Tenant context
     * @return Count
     */
    public long countPrescriptionsForPatient(UUID patientId, UUID tenantId) {
        return prescriptionRepository.countByPatientIdAndTenantId(patientId, tenantId);
    }

    /**
     * Custom exception for insufficient inventory
     */
    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) {
            super(message);
        }

        public InsufficientStockException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
