package com.clinic.backend.repository;

import com.clinic.common.entity.clinical.DrugInteraction;
import com.clinic.common.entity.clinical.DrugInteraction.InteractionSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Drug Interaction Repository - Persistence for drug interactions
 *
 * Custom queries for finding interactions between medication pairs
 * considering bidirectional lookups (A-B = B-A)
 */
@Repository
public interface DrugInteractionRepository extends JpaRepository<DrugInteraction, UUID> {

    /**
     * Find interaction between two medications (bidirectional).
     *
     * Looks for interaction where:
     * - medication_a_id = medId1 AND medication_b_id = medId2
     * OR
     * - medication_a_id = medId2 AND medication_b_id = medId1
     *
     * @param medId1 First medication ID
     * @param medId2 Second medication ID
     * @param tenantId Tenant context
     * @return Optional interaction if found
     */
    @Query("SELECT di FROM DrugInteraction di WHERE di.tenantId = :tenantId AND " +
           "((di.medicationA.id = :medId1 AND di.medicationB.id = :medId2) OR " +
           "(di.medicationA.id = :medId2 AND di.medicationB.id = :medId1))")
    Optional<DrugInteraction> findInteraction(
        @Param("medId1") UUID medId1,
        @Param("medId2") UUID medId2,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Find all interactions involving a specific medication as medication A
     *
     * @param medicationId Medication ID
     * @param tenantId Tenant context
     * @return List of interactions where this medication is A
     */
    List<DrugInteraction> findByMedicationAIdAndTenantId(UUID medicationId, UUID tenantId);

    /**
     * Find all interactions involving a specific medication as medication B
     *
     * @param medicationId Medication ID
     * @param tenantId Tenant context
     * @return List of interactions where this medication is B
     */
    List<DrugInteraction> findByMedicationBIdAndTenantId(UUID medicationId, UUID tenantId);

    /**
     * Find all interactions by severity
     *
     * @param severity Severity level
     * @param tenantId Tenant context
     * @return List of interactions with given severity
     */
    List<DrugInteraction> findBySeverityAndTenantId(InteractionSeverity severity, UUID tenantId);

    /**
     * Count interactions for a medication (both as A and B)
     *
     * @param medicationId Medication ID
     * @param tenantId Tenant context
     * @return Count of interactions
     */
    @Query("SELECT COUNT(di) FROM DrugInteraction di WHERE di.tenantId = :tenantId AND " +
           "(di.medicationA.id = :medId OR di.medicationB.id = :medId)")
    long countInteractionsForMedication(
        @Param("medId") UUID medicationId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Check if interaction exists between two medications
     *
     * @param medId1 First medication ID
     * @param medId2 Second medication ID
     * @param tenantId Tenant context
     * @return true if interaction exists
     */
    @Query("SELECT CASE WHEN COUNT(di) > 0 THEN true ELSE false END " +
           "FROM DrugInteraction di WHERE di.tenantId = :tenantId AND " +
           "((di.medicationA.id = :medId1 AND di.medicationB.id = :medId2) OR " +
           "(di.medicationA.id = :medId2 AND di.medicationB.id = :medId1))")
    boolean existsInteraction(
        @Param("medId1") UUID medId1,
        @Param("medId2") UUID medId2,
        @Param("tenantId") UUID tenantId
    );
}
