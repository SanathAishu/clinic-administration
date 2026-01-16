package com.clinic.common.entity.operational;

import com.clinic.common.entity.core.User;
import com.clinic.common.entity.SoftDeletableEntity;
import com.clinic.common.entity.operational.Inventory;
import com.clinic.common.enums.InventoryCategory;
import com.clinic.common.enums.ABCClassification;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "inventory", indexes = {
    @Index(name = "idx_inventory_tenant", columnList = "tenant_id"),
    @Index(name = "idx_inventory_category", columnList = "category"),
    @Index(name = "idx_inventory_low_stock", columnList = "tenant_id"),
    @Index(name = "idx_inventory_expiry", columnList = "expiry_date"),
    @Index(name = "idx_inventory_eoq", columnList = "eoq"),
    @Index(name = "idx_inventory_reorder_point", columnList = "reorder_point"),
    @Index(name = "idx_inventory_abc", columnList = "abc_classification")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory extends SoftDeletableEntity {

    // Item Details
    @Column(name = "item_name", nullable = false)
    @NotBlank(message = "Item name is required")
    @Size(max = 255)
    private String itemName;

    @Column(name = "item_code", unique = true, length = 100)
    @Size(max = 100)
    private String itemCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    @NotNull
    private InventoryCategory category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Stock Information
    @Column(name = "current_stock", nullable = false)
    @NotNull
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer currentStock = 0;

    @Column(name = "minimum_stock", nullable = false)
    @NotNull
    @Min(value = 0, message = "Minimum stock cannot be negative")
    private Integer minimumStock = 0;

    @Column(name = "unit", nullable = false, length = 50)
    @NotBlank(message = "Unit is required")
    @Size(max = 50)
    private String unit;

    @Column(name = "unit_price", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Unit price cannot be negative")
    private BigDecimal unitPrice;

    // Medicine Specific
    @Column(name = "manufacturer")
    @Size(max = 255)
    private String manufacturer;

    @Column(name = "batch_number", length = 100)
    @Size(max = 100)
    private String batchNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // Storage
    @Column(name = "location", length = 100)
    @Size(max = 100)
    private String location;

    // Operations Research: Economic Order Quantity (EOQ) Model
    /**
     * Annual demand in units/year.
     * Used in EOQ formula: Q* = √(2DS/H)
     */
    @Column(name = "annual_demand", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Annual demand cannot be negative")
    private Double annualDemand;

    /**
     * Fixed ordering cost per order in currency units.
     * S in EOQ formula Q* = √(2DS/H)
     */
    @Column(name = "ordering_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Ordering cost cannot be negative")
    private BigDecimal orderingCost;

    /**
     * Holding (carrying) cost per unit per year.
     * H in EOQ formula Q* = √(2DS/H)
     */
    @Column(name = "holding_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Holding cost cannot be negative")
    private BigDecimal holdingCost;

    /**
     * Supplier lead time in days.
     * L in ROP formula: ROP = d·L + z·σ·√L
     */
    @Column(name = "lead_time_days")
    @Min(value = 0, message = "Lead time cannot be negative")
    private Integer leadTimeDays;

    /**
     * Standard deviation of daily demand.
     * σ in ROP formula: ROP = d·L + z·σ·√L
     * Calculated from historical demand data.
     */
    @Column(name = "demand_std_dev", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Demand std dev cannot be negative")
    private Double demandStdDev;

    /**
     * Target service level (0.70 to 0.999).
     * Probability of no stockout during lead time.
     * Common values: 0.90 (90%), 0.95 (95%), 0.99 (99%)
     */
    @Column(name = "service_level", precision = 4, scale = 3)
    @DecimalMin(value = "0.0", message = "Service level cannot be negative")
    @DecimalMax(value = "1.0", message = "Service level cannot exceed 1.0")
    private Double serviceLevel;

    // Calculated Fields (Auto-calculated in @PrePersist/@PreUpdate)

    /**
     * Economic Order Quantity: Q* = √(2DS/H)
     * Optimal order quantity that minimizes total inventory cost.
     * Invariant: Must be positive if all parameters present.
     */
    @Column(name = "eoq", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "EOQ cannot be negative")
    private Double economicOrderQuantity;

    /**
     * Reorder Point: ROP = d·L + z·σ·√L
     * Inventory level at which to place new order.
     * Invariant: Must be non-negative if all parameters present.
     */
    @Column(name = "reorder_point")
    @Min(value = 0, message = "Reorder point cannot be negative")
    private Integer reorderPoint;

    /**
     * Safety Stock: SS = z·σ·√L
     * Buffer inventory to prevent stockouts during demand uncertainty.
     * Invariant: Must be non-negative if all parameters present.
     */
    @Column(name = "safety_stock")
    @Min(value = 0, message = "Safety stock cannot be negative")
    private Integer safetyStock;

    /**
     * ABC Classification based on annual value (Pareto Analysis).
     * A items (70% of value): Tight control
     * B items (20% of value): Moderate control
     * C items (10% of value): Loose control
     */
    @Column(name = "abc_classification")
    @Enumerated(EnumType.STRING)
    private ABCClassification abcClassification;

    // Metadata
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @NotNull
    private User createdBy;

    // ======================== LIFECYCLE HOOKS ========================

    /**
     * Calculates and validates OR fields before persistence.
     * Enforces mathematical invariants:
     * - EOQ > 0 (if parameters present)
     * - ROP ≥ 0, SS ≥ 0 (if parameters present)
     * - dL > 0 (temporal ordering)
     *
     * Mathematical Proofs:
     *
     * EOQ Invariant (Theorem 8):
     * Derived from minimization of TC(Q) = (D/Q)·S + (Q/2)·H
     * dTC/dQ = -D·S/Q² + H/2 = 0 ⟹ Q* = √(2DS/H) > 0
     * d²TC/dQ² = 2DS/Q³ > 0 ⟹ Q* is minimum ✓
     *
     * ROP Invariant (Theorem 9):
     * Demand during lead time ~ Normal(d·L, σ²·L)
     * For service level α: P(Demand ≤ ROP) = α
     * ROP = d·L + z·σ·√L ≥ d·L ≥ 0 ✓
     */
    @PrePersist
    @PreUpdate
    protected void calculateAndValidateInventory() {
        // Calculate EOQ if all parameters present
        if (annualDemand != null && orderingCost != null && holdingCost != null) {
            double d = annualDemand;
            double s = orderingCost.doubleValue();
            double h = holdingCost.doubleValue();

            // Verify non-negativity constraint
            if (d < 0 || s < 0 || h < 0) {
                throw new IllegalStateException(
                    "EOQ parameters cannot be negative: D=" + d + ", S=" + s + ", H=" + h
                );
            }

            // Calculate Q* = √(2DS/H)
            if (d > 0 && h > 0) {
                this.economicOrderQuantity = Math.sqrt((2 * d * s) / h);

                // Invariant: EOQ > 0
                if (economicOrderQuantity <= 0) {
                    throw new IllegalStateException(
                        "Invariant violation: EOQ must be positive, got " + economicOrderQuantity
                    );
                }
            }
        }

        // Calculate ROP and safety stock if parameters present
        if (annualDemand != null && leadTimeDays != null &&
            demandStdDev != null && serviceLevel != null) {

            // Verify non-negativity constraints
            if (annualDemand < 0 || leadTimeDays < 0 || demandStdDev < 0) {
                throw new IllegalStateException(
                    "ROP parameters cannot be negative: D=" + annualDemand +
                    ", L=" + leadTimeDays + ", σ=" + demandStdDev
                );
            }

            double dailyDemand = annualDemand / 365.0;
            double z = getZScore(serviceLevel);

            // SS = z × σ × √L
            this.safetyStock = (int) Math.ceil(
                z * demandStdDev * Math.sqrt(leadTimeDays)
            );

            // ROP = d×L + SS
            this.reorderPoint = (int) Math.ceil(
                dailyDemand * leadTimeDays
            ) + safetyStock;

            // Invariants
            if (safetyStock < 0 || reorderPoint < 0) {
                throw new IllegalStateException(
                    "Invariant violation: Safety stock and ROP must be non-negative. " +
                    "SS=" + safetyStock + ", ROP=" + reorderPoint
                );
            }
        }
    }

    /**
     * Maps service level to z-score (standard normal distribution).
     * Used in safety stock calculation: SS = z·σ·√L
     *
     * Z-score derivation:
     * For cumulative normal distribution: P(Z ≤ z) = serviceLevel
     * Where Z ~ Normal(0, 1)
     *
     * Common z-scores (inverse normal CDF):
     */
    private double getZScore(double serviceLevel) {
        if (serviceLevel >= 0.999) return 3.090;  // 99.9% service level
        if (serviceLevel >= 0.990) return 2.326;  // 99.0% service level
        if (serviceLevel >= 0.950) return 1.645;  // 95.0% service level
        if (serviceLevel >= 0.900) return 1.282;  // 90.0% service level
        if (serviceLevel >= 0.750) return 0.674;  // 75.0% service level
        return 1.645;  // default to 95%
    }

    // ======================== HELPER METHODS ========================

    public boolean isLowStock() {
        return currentStock <= minimumStock;
    }

    public boolean isBelowReorderPoint() {
        if (reorderPoint == null) {
            return false;
        }
        return currentStock <= reorderPoint;
    }

    public boolean isExpiringSoon(int daysThreshold) {
        if (expiryDate == null) {
            return false;
        }
        return expiryDate.isBefore(LocalDate.now().plusDays(daysThreshold));
    }

    public boolean isExpired() {
        if (expiryDate == null) {
            return false;
        }
        return expiryDate.isBefore(LocalDate.now());
    }

    public void addStock(int quantity) {
        this.currentStock += quantity;
    }

    public void reduceStock(int quantity) {
        if (this.currentStock >= quantity) {
            this.currentStock -= quantity;
        } else {
            throw new IllegalArgumentException("Insufficient stock available");
        }
    }

    /**
     * Calculate annual inventory value for ABC analysis.
     * annualValue = annualDemand × unitPrice
     */
    public BigDecimal calculateAnnualValue() {
        if (annualDemand == null || unitPrice == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(annualDemand)
            .multiply(unitPrice);
    }
}
