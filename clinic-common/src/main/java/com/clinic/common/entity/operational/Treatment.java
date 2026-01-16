package com.clinic.common.entity.operational;

import com.clinic.common.entity.SoftDeletableEntity;
import com.clinic.common.entity.core.Branch;
import com.clinic.common.entity.core.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Treatment and Service Catalog Entity
 * Stores treatments, physiotherapy services, and other medical services
 * Supports pricing, discounts, and service duration tracking
 */
@Entity
@Table(name = "treatments", indexes = {
    @Index(name = "idx_treatments_tenant", columnList = "tenant_id"),
    @Index(name = "idx_treatments_name", columnList = "name"),
    @Index(name = "idx_treatments_category", columnList = "category"),
    @Index(name = "idx_treatments_active", columnList = "is_active"),
    @Index(name = "idx_treatments_branch", columnList = "branch_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Treatment extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "name", nullable = false, length = 255)
    @NotBlank(message = "Treatment name is required")
    @Size(max = 255, message = "Treatment name cannot exceed 255 characters")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 100)
    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;

    // Pricing
    @Column(name = "base_cost", precision = 10, scale = 2, nullable = false)
    @NotNull(message = "Base cost is required")
    @DecimalMin(value = "0.0", message = "Base cost cannot be negative")
    private BigDecimal baseCost;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    @DecimalMin(value = "0.0", message = "Discount percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100")
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    // Duration (in minutes, optional)
    @Column(name = "duration_minutes")
    @Min(value = 0, message = "Duration cannot be negative")
    private Integer durationMinutes;

    // Status
    @Column(name = "is_active", nullable = false)
    @NotNull
    @Builder.Default
    private Boolean isActive = true;

    // Additional Information
    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "prerequisites", columnDefinition = "TEXT")
    private String prerequisites;

    // Metadata
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @NotNull
    private User createdBy;

    /**
     * Calculate final cost after discount
     * Formula: finalCost = baseCost * (1 - discountPercentage/100)
     */
    public BigDecimal calculateFinalCost() {
        if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) == 0) {
            return baseCost;
        }

        BigDecimal discountMultiplier = BigDecimal.ONE
            .subtract(discountPercentage.divide(new BigDecimal("100"), 4, BigDecimal.ROUND_HALF_UP));

        return baseCost.multiply(discountMultiplier)
            .setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Get discount amount
     */
    public BigDecimal getDiscountAmount() {
        if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return baseCost.subtract(calculateFinalCost())
            .setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Invariant validation (Discrete Math: Invariants)
     */
    @PrePersist
    @PreUpdate
    protected void validateInvariants() {
        // Invariant: Discount percentage must be between 0 and 100
        if (discountPercentage != null &&
            (discountPercentage.compareTo(BigDecimal.ZERO) < 0 ||
             discountPercentage.compareTo(new BigDecimal("100")) > 0)) {
            throw new IllegalStateException(
                String.format("Invariant violation: Discount percentage must be between 0 and 100, got %.2f",
                    discountPercentage)
            );
        }

        // Invariant: Base cost must be non-negative
        if (baseCost != null && baseCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                String.format("Invariant violation: Base cost cannot be negative, got %.2f", baseCost)
            );
        }
    }
}
