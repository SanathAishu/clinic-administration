package com.clinic.common.entity.clinical;

import com.clinic.common.entity.TenantAwareEntity;
import com.clinic.common.entity.operational.Inventory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Drug Interaction Entity - Represents interactions between two medications
 *
 * Tracks potential adverse drug interactions to prevent dangerous medication combinations.
 * Checked during prescription dispensing to ensure patient safety.
 *
 * Features:
 * - Bidirectional interaction lookup (medicationA with medicationB = medicationB with medicationA)
 * - Severity levels: MINOR, MODERATE, SEVERE
 * - Clinical recommendations for managing interactions
 * - Unique constraint on medication pair (A, B)
 *
 * Reference: Phase D Feature 3 - Prescription Enhancement
 */
@Entity
@Table(
    name = "drug_interactions",
    indexes = {
        @Index(name = "idx_drug_interactions_medications", columnList = "medication_a_id, medication_b_id"),
        @Index(name = "idx_drug_interactions_severity", columnList = "severity"),
        @Index(name = "idx_drug_interactions_tenant", columnList = "tenant_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_drug_interactions_pair",
            columnNames = {"tenant_id", "medication_a_id", "medication_b_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrugInteraction extends TenantAwareEntity {

    /**
     * First medication in the interaction pair
     * Should be the medication with lower ID for consistency
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_a_id", nullable = false)
    @NotNull
    private Inventory medicationA;

    /**
     * Second medication in the interaction pair
     * Should be the medication with higher ID for consistency
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_b_id", nullable = false)
    @NotNull
    private Inventory medicationB;

    /**
     * Severity level of the interaction
     * - MINOR: Monitor patient, unlikely to cause harm
     * - MODERATE: Dosage adjustment may be needed, possible harmful effect
     * - SEVERE: Avoid combination or use with extreme caution, high risk
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    @NotNull
    private InteractionSeverity severity;

    /**
     * Clinical description of the interaction
     * Explains what happens when these medications are combined
     * Examples:
     * - "May increase risk of bleeding"
     * - "May reduce effectiveness of both drugs"
     * - "Risk of severe hypotension"
     */
    @Column(name = "description", columnDefinition = "TEXT")
    @NotBlank(message = "Interaction description is required")
    private String description;

    /**
     * Recommendation for managing the interaction
     * Clinical guidance on how to handle the combination
     * Examples:
     * - "Monitor blood glucose levels closely"
     * - "Increase dosage interval by 2 hours"
     * - "Consider alternative medication"
     * - "Contraindicated - do not combine"
     */
    @Column(name = "recommendation", columnDefinition = "TEXT")
    @NotBlank(message = "Recommendation is required")
    private String recommendation;

    /**
     * Enumeration of interaction severity levels
     *
     * MINOR: Low-risk interactions
     *   - Patient monitoring sufficient
     *   - Usually no dose adjustment needed
     *   - Log warning for awareness
     *
     * MODERATE: Medium-risk interactions
     *   - Dose adjustment may be required
     *   - Close monitoring recommended
     *   - Clinical judgment needed
     *   - Log warning - allow with caution
     *
     * SEVERE: High-risk interactions
     *   - Combination should be avoided
     *   - Only if absolutely necessary
     *   - Close medical supervision required
     *   - Block dispensing - require manual override
     */
    public enum InteractionSeverity {
        /**
         * Minor interaction: Monitor patient, unlikely to cause significant harm
         * Log action: WARNING level
         * Dispensing: ALLOWED
         */
        MINOR,

        /**
         * Moderate interaction: Possible harmful effects, dosage adjustment may be needed
         * Log action: WARNING level with detailed description
         * Dispensing: ALLOWED with warning to dispensing user
         */
        MODERATE,

        /**
         * Severe interaction: Avoid combination or use with extreme caution
         * Log action: ERROR level
         * Dispensing: BLOCKED - requires manual override by authorized user
         */
        SEVERE
    }
}
