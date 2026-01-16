package com.clinic.backend.service;

import com.clinic.common.entity.clinical.DrugInteraction;
import com.clinic.common.entity.clinical.DrugInteraction.InteractionSeverity;
import com.clinic.common.entity.clinical.PrescriptionItem;
import com.clinic.common.entity.patient.Patient;
import com.clinic.backend.repository.DrugInteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Drug Interaction Service - Checks for dangerous medication combinations
 *
 * Performs interaction checks during prescription dispensing to ensure patient safety.
 * Maintains registry of known drug interactions and their severity levels.
 *
 * Reference: Phase D Feature 3 - Prescription Enhancement
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DrugInteractionService {

    private final DrugInteractionRepository drugInteractionRepository;

    /**
     * Check for drug interactions in a list of prescription items.
     *
     * Examines all pairwise combinations of medications in the prescription.
     * Raises exceptions for SEVERE interactions, logs warnings for MODERATE/MINOR.
     *
     * Algorithm:
     * For each pair of medications (i, j) where i < j:
     *   1. Query for interaction (med_i, med_j) in database
     *   2. If found, check severity:
     *      - SEVERE: throw DrugInteractionException
     *      - MODERATE: log warning
     *      - MINOR: log info
     *
     * @param items Medications in prescription
     * @param patient Patient receiving medications
     * @throws DrugInteractionException if SEVERE interaction detected
     */
    public void checkDrugInteractions(List<PrescriptionItem> items, Patient patient) {
        log.debug("Checking drug interactions for patient: {} with {} medications",
            patient.getId(), items.size());

        // Early exit if only one medication (no interactions possible)
        if (items == null || items.size() < 2) {
            log.debug("No interactions possible with {} medications", items == null ? 0 : items.size());
            return;
        }

        List<DrugInteraction> severeInteractions = new ArrayList<>();
        List<DrugInteraction> moderateInteractions = new ArrayList<>();
        List<DrugInteraction> minorInteractions = new ArrayList<>();

        // Check all pairwise combinations
        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                PrescriptionItem itemA = items.get(i);
                PrescriptionItem itemB = items.get(j);

                // Query database for interaction
                // Check both orderings since interaction may be stored as (A,B) or (B,A)
                Optional<DrugInteraction> interaction = findInteraction(
                    itemA.getInventory().getId(),
                    itemB.getInventory().getId(),
                    patient.getTenantId()
                );

                if (interaction.isPresent()) {
                    DrugInteraction di = interaction.get();
                    logInteraction(di, itemA, itemB, patient);

                    // Categorize by severity
                    switch (di.getSeverity()) {
                        case SEVERE -> severeInteractions.add(di);
                        case MODERATE -> moderateInteractions.add(di);
                        case MINOR -> minorInteractions.add(di);
                    }
                }
            }
        }

        // Throw exception for SEVERE interactions (fail-fast)
        if (!severeInteractions.isEmpty()) {
            DrugInteraction severe = severeInteractions.get(0);
            String message = String.format(
                "SEVERE drug interaction detected: %s (%s) + %s (%s) - %s",
                severe.getMedicationA().getItemName(),
                severe.getMedicationA().getId(),
                severe.getMedicationB().getItemName(),
                severe.getMedicationB().getId(),
                severe.getDescription()
            );
            log.error("{} for patient {}", message, patient.getId());
            throw new DrugInteractionException(message, severe);
        }

        // Log MODERATE interactions (warnings)
        for (DrugInteraction interaction : moderateInteractions) {
            log.warn("MODERATE interaction for patient {}: {} + {} - {}",
                patient.getId(),
                interaction.getMedicationA().getItemName(),
                interaction.getMedicationB().getItemName(),
                interaction.getDescription());
        }

        // Log MINOR interactions (info level)
        for (DrugInteraction interaction : minorInteractions) {
            log.info("MINOR interaction for patient {}: {} + {} - {}",
                patient.getId(),
                interaction.getMedicationA().getItemName(),
                interaction.getMedicationB().getItemName(),
                interaction.getDescription());
        }
    }

    /**
     * Find interaction between two medications (handles bidirectional lookup).
     *
     * Since interactions are symmetric (A↔B = B↔A), we check:
     * 1. First try: (medA, medB)
     * 2. If not found: try (medB, medA)
     *
     * @param medId1 First medication ID
     * @param medId2 Second medication ID
     * @param tenantId Tenant context
     * @return Optional containing interaction if found
     */
    private Optional<DrugInteraction> findInteraction(UUID medId1, UUID medId2, UUID tenantId) {
        // Try first ordering
        Optional<DrugInteraction> interaction = drugInteractionRepository
            .findInteraction(medId1, medId2, tenantId);

        // If not found, try reverse ordering
        if (interaction.isEmpty()) {
            interaction = drugInteractionRepository
                .findInteraction(medId2, medId1, tenantId);
        }

        return interaction;
    }

    /**
     * Log interaction details for audit trail.
     *
     * @param interaction The drug interaction
     * @param itemA First prescription item
     * @param itemB Second prescription item
     * @param patient Patient receiving medications
     */
    private void logInteraction(DrugInteraction interaction, PrescriptionItem itemA,
                               PrescriptionItem itemB, Patient patient) {
        log.debug("Found {} drug interaction for patient {}: {} ({} {}) + {} ({} {})",
            interaction.getSeverity(),
            patient.getId(),
            itemA.getInventory().getItemName(),
            itemA.getDosage(),
            itemA.getDosageUnit(),
            itemB.getInventory().getItemName(),
            itemB.getDosage(),
            itemB.getDosageUnit());
    }

    /**
     * Get or create drug interaction record.
     *
     * @param medicationA First medication
     * @param medicationB Second medication
     * @param severity Severity level
     * @param description Interaction description
     * @param recommendation Clinical recommendation
     * @param tenantId Tenant context
     * @return Saved or existing drug interaction
     */
    @Transactional
    public DrugInteraction createOrUpdateInteraction(
            UUID medicationA,
            UUID medicationB,
            InteractionSeverity severity,
            String description,
            String recommendation,
            UUID tenantId) {

        Optional<DrugInteraction> existing = drugInteractionRepository
            .findInteraction(medicationA, medicationB, tenantId);

        if (existing.isPresent()) {
            DrugInteraction interaction = existing.get();
            interaction.setSeverity(severity);
            interaction.setDescription(description);
            interaction.setRecommendation(recommendation);
            log.info("Updated drug interaction: {} <-> {}", medicationA, medicationB);
            return drugInteractionRepository.save(interaction);
        } else {
            // Create new interaction using smaller ID as A and larger as B for consistency
            UUID medA, medB;
            if (medicationA.compareTo(medicationB) < 0) {
                medA = medicationA;
                medB = medicationB;
            } else {
                medA = medicationB;
                medB = medicationA;
            }

            com.clinic.common.entity.operational.Inventory invA = new com.clinic.common.entity.operational.Inventory();
            invA.setId(medA);
            com.clinic.common.entity.operational.Inventory invB = new com.clinic.common.entity.operational.Inventory();
            invB.setId(medB);

            DrugInteraction interaction = DrugInteraction.builder()
                .medicationA(invA)
                .medicationB(invB)
                .severity(severity)
                .description(description)
                .recommendation(recommendation)
                .build();
            interaction.setTenantId(tenantId);

            log.info("Created new drug interaction: {} <-> {}", medA, medB);
            return drugInteractionRepository.save(interaction);
        }
    }

    /**
     * Get all interactions for a medication.
     *
     * @param medicationId Medication ID
     * @param tenantId Tenant context
     * @return List of all interactions involving this medication
     */
    public List<DrugInteraction> getInteractionsForMedication(UUID medicationId, UUID tenantId) {
        List<DrugInteraction> interactions = new ArrayList<>();

        // Get interactions where this is medication A
        interactions.addAll(drugInteractionRepository.findByMedicationAIdAndTenantId(medicationId, tenantId));

        // Get interactions where this is medication B
        interactions.addAll(drugInteractionRepository.findByMedicationBIdAndTenantId(medicationId, tenantId));

        return interactions;
    }

    /**
     * Get all severe interactions for a medication.
     *
     * @param medicationId Medication ID
     * @param tenantId Tenant context
     * @return List of severe interactions
     */
    public List<DrugInteraction> getSevereInteractionsForMedication(UUID medicationId, UUID tenantId) {
        return getInteractionsForMedication(medicationId, tenantId).stream()
            .filter(di -> di.getSeverity() == InteractionSeverity.SEVERE)
            .collect(Collectors.toList());
    }

    /**
     * Remove drug interaction record.
     *
     * @param medicationA First medication
     * @param medicationB Second medication
     * @param tenantId Tenant context
     */
    @Transactional
    public void deleteInteraction(UUID medicationA, UUID medicationB, UUID tenantId) {
        Optional<DrugInteraction> interaction = drugInteractionRepository
            .findInteraction(medicationA, medicationB, tenantId);

        if (interaction.isPresent()) {
            drugInteractionRepository.delete(interaction.get());
            log.info("Deleted drug interaction: {} <-> {}", medicationA, medicationB);
        }
    }

    /**
     * Custom exception for drug interactions
     */
    public static class DrugInteractionException extends RuntimeException {
        private final DrugInteraction interaction;

        public DrugInteractionException(String message, DrugInteraction interaction) {
            super(message);
            this.interaction = interaction;
        }

        public DrugInteraction getInteraction() {
            return interaction;
        }
    }
}
