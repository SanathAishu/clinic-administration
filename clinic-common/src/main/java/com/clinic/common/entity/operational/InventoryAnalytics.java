package com.clinic.common.entity.operational;

import com.clinic.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Inventory Analytics Entity - Tracks demand statistics for inventory optimization.
 *
 * Purpose: Maintain historical demand data to calculate:
 * - Daily demand mean (d) for ROP calculation
 * - Demand standard deviation (σ) for safety stock
 * - Demand patterns for forecasting
 *
 * Mathematical Foundation:
 * Reorder Point formula requires: ROP = d·L + z·σ·√L
 * Where:
 * - d = average daily demand (units/day)
 * - σ = standard deviation of daily demand (units)
 * - L = lead time (days)
 * - z = z-score for service level
 *
 * This entity aggregates demand statistics over a period.
 * Multiple analytics entries form a time series for demand forecasting.
 */
@Entity
@Table(name = "inventory_analytics", indexes = {
    @Index(name = "idx_inventory_analytics_tenant", columnList = "tenant_id"),
    @Index(name = "idx_inventory_analytics_inventory", columnList = "inventory_id, period_start DESC"),
    @Index(name = "idx_inventory_analytics_period", columnList = "period_start, period_end")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAnalytics extends SoftDeletableEntity {

    /**
     * Reference to the inventory item.
     * Forms the aggregation basis for demand statistics.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    @NotNull
    private Inventory inventory;

    /**
     * Analysis period start date (inclusive).
     * Invariant: periodStart ≤ periodEnd
     */
    @Column(name = "period_start", nullable = false)
    @NotNull
    @PastOrPresent(message = "Period start cannot be in the future")
    private LocalDate periodStart;

    /**
     * Analysis period end date (inclusive).
     * Invariant: periodEnd ≥ periodStart
     */
    @Column(name = "period_end", nullable = false)
    @NotNull
    @PastOrPresent(message = "Period end cannot be in the future")
    private LocalDate periodEnd;

    /**
     * Total demand quantity during the period (units).
     * Sum of all quantity consumed/sold in this period.
     * Invariant: totalDemand ≥ 0
     */
    @Column(name = "total_demand", nullable = false)
    @NotNull
    @Min(value = 0, message = "Total demand cannot be negative")
    private Integer totalDemand;

    /**
     * Average daily demand during the period.
     * d in ROP formula: ROP = d·L + z·σ·√L
     * Calculated as: avgDailyDemand = totalDemand / (periodEnd - periodStart)
     * Invariant: avgDailyDemand ≥ 0
     */
    @Column(name = "avg_daily_demand", precision = 10, scale = 4)
    @NotNull
    @DecimalMin(value = "0.0", message = "Average daily demand cannot be negative")
    private Double avgDailyDemand;

    /**
     * Standard deviation of daily demand.
     * σ in ROP formula: ROP = d·L + z·σ·√L
     * Measures demand variability/uncertainty.
     * High σ → High safety stock needed
     * Invariant: demandStdDev ≥ 0
     */
    @Column(name = "demand_std_dev", precision = 10, scale = 4)
    @NotNull
    @DecimalMin(value = "0.0", message = "Demand std dev cannot be negative")
    private Double demandStdDev;

    /**
     * Minimum daily demand observed during the period.
     * Lower bound of demand variability.
     * Used for capacity planning and baseline estimation.
     */
    @Column(name = "min_daily_demand")
    @Min(value = 0, message = "Minimum daily demand cannot be negative")
    private Integer minDailyDemand;

    /**
     * Maximum daily demand observed during the period.
     * Upper bound of demand variability.
     * Used to detect demand spikes and set upper safety stock bounds.
     * Invariant: maxDailyDemand ≥ minDailyDemand
     */
    @Column(name = "max_daily_demand")
    @Min(value = 0, message = "Maximum daily demand cannot be negative")
    private Integer maxDailyDemand;

    // ======================== LIFECYCLE HOOKS ========================

    /**
     * Validates temporal and statistical invariants.
     * Enforces discrete mathematics principles:
     * - Total order on temporal dimension: periodStart ≤ periodEnd
     * - Monotonic sequences: minDailyDemand ≤ maxDailyDemand
     * - Non-negative measures: all demands ≥ 0
     */
    @PrePersist
    @PreUpdate
    protected void validateAnalytics() {
        // Temporal invariant: periodStart ≤ periodEnd
        if (periodStart.isAfter(periodEnd)) {
            throw new IllegalStateException(
                "Invariant violation: Period start (" + periodStart + ") must be ≤ period end (" +
                periodEnd + ")"
            );
        }

        // Demand min/max invariant: minDailyDemand ≤ maxDailyDemand
        if (minDailyDemand != null && maxDailyDemand != null &&
            minDailyDemand > maxDailyDemand) {
            throw new IllegalStateException(
                "Invariant violation: Minimum daily demand (" + minDailyDemand +
                ") must be ≤ maximum daily demand (" + maxDailyDemand + ")"
            );
        }

        // Verify avgDailyDemand is consistent with totalDemand
        if (totalDemand >= 0) {
            long daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd);
            if (daysInPeriod == 0) {
                daysInPeriod = 1; // At least 1 day period
            }

            double expectedAvg = totalDemand / (double) daysInPeriod;
            double tolerance = 0.01; // Allow 1% tolerance

            if (Math.abs(avgDailyDemand - expectedAvg) > tolerance) {
                throw new IllegalStateException(
                    "Invariant violation: avgDailyDemand (" + avgDailyDemand +
                    ") does not match totalDemand/days (" + expectedAvg + ")"
                );
            }
        }
    }

    // ======================== HELPER METHODS ========================

    /**
     * Get the number of days in the analysis period.
     */
    public long getPeriodDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
    }

    /**
     * Calculate demand coefficient of variation (CV = σ/μ).
     * Indicates demand stability:
     * - CV < 0.5: Stable demand
     * - 0.5 ≤ CV < 1.0: Moderate variability
     * - CV ≥ 1.0: High variability
     */
    public Double getCoefficientOfVariation() {
        if (avgDailyDemand == null || avgDailyDemand == 0 || demandStdDev == null) {
            return null;
        }
        return demandStdDev / avgDailyDemand;
    }

    /**
     * Check if demand is stable (CV < 0.5).
     * Stable demands allow for lower safety stocks.
     */
    public boolean isStableDemand() {
        Double cv = getCoefficientOfVariation();
        return cv != null && cv < 0.5;
    }

    /**
     * Check if demand is highly variable (CV ≥ 1.0).
     * Highly variable demands require higher safety stocks.
     */
    public boolean isHighlyVariableDemand() {
        Double cv = getCoefficientOfVariation();
        return cv != null && cv >= 1.0;
    }
}
