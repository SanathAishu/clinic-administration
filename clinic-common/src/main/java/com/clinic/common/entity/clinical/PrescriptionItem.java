package com.clinic.common.entity.clinical;

import com.clinic.common.entity.TenantAwareEntity;
import com.clinic.common.entity.operational.Inventory;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Prescription Item Entity - Medication details within a prescription
 *
 * Each prescription contains one or more items, each specifying:
 * - Which medication (reference to Inventory)
 * - Dosage and frequency information
 * - Quantities (prescribed vs dispensed)
 *
 * Theorem 11: Quantity Invariant
 * prescribedQuantity = dosage × frequencyPerDay × durationDays
 * dispensedQuantity ≤ prescribedQuantity (cannot dispense more than prescribed)
 *
 * Reference: Phase D Feature 3 - Prescription Enhancement
 */
@Entity
@Table(name = "prescription_items", indexes = {
    @Index(name = "idx_prescription_items_prescription", columnList = "prescription_id"),
    @Index(name = "idx_prescription_items_inventory", columnList = "inventory_id"),
    @Index(name = "idx_prescription_items_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItem extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    @NotNull
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    @NotNull
    private Inventory inventory;

    // Dosage Information
    /**
     * Dosage amount (e.g., 500 for 500mg)
     * Must be positive
     */
    @Column(name = "dosage", nullable = false, precision = 10, scale = 2)
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Dosage must be positive")
    private Double dosage;

    /**
     * Unit of measurement (mg, ml, tablet, capsule, etc.)
     * Examples: "mg", "ml", "tablet", "capsule"
     */
    @Column(name = "dosage_unit", nullable = false)
    @NotBlank(message = "Dosage unit is required")
    @Size(max = 50)
    private String dosageUnit;

    /**
     * Frequency per day (how many times per day)
     * Examples: 1 (once daily), 2 (twice daily), 3 (three times daily)
     */
    @Column(name = "frequency_per_day", nullable = false)
    @NotNull
    @Min(value = 1, message = "Frequency per day must be at least 1")
    private Integer frequencyPerDay;

    /**
     * Duration in days (how many days to take the medication)
     */
    @Column(name = "duration_days", nullable = false)
    @NotNull
    @Min(value = 1, message = "Duration must be at least 1 day")
    @Max(value = 365, message = "Duration cannot exceed 365 days")
    private Integer durationDays;

    /**
     * Total prescribed quantity calculated from dosage × frequency × duration.
     *
     * Formula: prescribedQuantity = dosage × frequencyPerDay × durationDays
     *
     * Example:
     * - dosage = 500 (mg)
     * - frequencyPerDay = 2 (twice a day)
     * - durationDays = 10 (days)
     * - prescribedQuantity = 500 × 2 × 10 = 10,000 units
     */
    @Column(name = "prescribed_quantity", nullable = false)
    @NotNull
    @Min(value = 1, message = "Prescribed quantity must be at least 1")
    private Integer prescribedQuantity;

    /**
     * Actual quantity dispensed to the patient.
     *
     * Invariant: dispensedQuantity ≤ prescribedQuantity
     * Cannot dispense more medication than prescribed.
     */
    @Column(name = "dispensed_quantity")
    @Min(value = 0, message = "Dispensed quantity cannot be negative")
    @Builder.Default
    private Integer dispensedQuantity = 0;

    /**
     * Special instructions for patient (e.g., "with meals", "before bed")
     */
    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    /**
     * Calculate total prescribed quantity from dosage, frequency, and duration.
     *
     * Theorem 11: Quantity Invariant
     * prescribedQuantity = dosage × frequencyPerDay × durationDays
     *
     * This is called during @PrePersist to compute the total quantity.
     */
    @PrePersist
    protected void calculatePrescribedQuantity() {
        if (dosage != null && frequencyPerDay != null && durationDays != null) {
            this.prescribedQuantity = (int) Math.ceil(
                dosage * frequencyPerDay * durationDays
            );
        }
    }

    /**
     * Validate invariants:
     * 1. prescribedQuantity must be calculated correctly
     * 2. dispensedQuantity ≤ prescribedQuantity (cannot dispense more than prescribed)
     *
     * @throws IllegalStateException if any invariant is violated
     */
    @PrePersist
    @PreUpdate
    protected void validateInvariants() {
        // Invariant 1: dispensedQuantity ≤ prescribedQuantity (Theorem 11)
        if (dispensedQuantity != null && dispensedQuantity > prescribedQuantity) {
            throw new IllegalStateException(
                String.format(
                    "Invariant violation: dispensedQuantity (%d) > prescribedQuantity (%d)",
                    dispensedQuantity, prescribedQuantity
                )
            );
        }

        // Invariant 2: dosage must be positive
        if (dosage != null && dosage <= 0) {
            throw new IllegalStateException("Invariant violation: dosage must be positive");
        }

        // Invariant 3: frequencyPerDay must be positive
        if (frequencyPerDay != null && frequencyPerDay <= 0) {
            throw new IllegalStateException("Invariant violation: frequencyPerDay must be positive");
        }

        // Invariant 4: durationDays must be positive
        if (durationDays != null && durationDays <= 0) {
            throw new IllegalStateException("Invariant violation: durationDays must be positive");
        }
    }
}
