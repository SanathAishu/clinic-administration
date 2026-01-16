package com.clinic.backend.dto.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for Inventory Analytics data.
 *
 * Represents aggregated demand statistics for an inventory item over a period.
 * Used to populate OR calculation parameters:
 * - avgDailyDemand → ROP calculation
 * - demandStdDev → Safety stock calculation
 * - minDailyDemand, maxDailyDemand → Demand variability assessment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAnalyticsDTO {

    /**
     * Analytics record ID.
     */
    @JsonProperty("analytics_id")
    private UUID analyticsId;

    /**
     * Inventory item ID.
     */
    @JsonProperty("inventory_id")
    private UUID inventoryId;

    /**
     * Item name for reference.
     */
    @JsonProperty("item_name")
    private String itemName;

    /**
     * Analysis period start date.
     */
    @JsonProperty("period_start")
    private LocalDate periodStart;

    /**
     * Analysis period end date.
     */
    @JsonProperty("period_end")
    private LocalDate periodEnd;

    /**
     * Number of days in the analysis period.
     */
    @JsonProperty("period_days")
    private Long periodDays;

    /**
     * Total demand during the period (units).
     */
    @JsonProperty("total_demand")
    private Integer totalDemand;

    /**
     * Average daily demand (units/day).
     * d in ROP formula: ROP = d·L + z·σ·√L
     */
    @JsonProperty("avg_daily_demand")
    private Double avgDailyDemand;

    /**
     * Standard deviation of daily demand (units).
     * σ in ROP formula: ROP = d·L + z·σ·√L
     */
    @JsonProperty("demand_std_dev")
    private Double demandStdDev;

    /**
     * Minimum daily demand observed (units).
     * Lower bound of demand variability.
     */
    @JsonProperty("min_daily_demand")
    private Integer minDailyDemand;

    /**
     * Maximum daily demand observed (units).
     * Upper bound of demand variability.
     */
    @JsonProperty("max_daily_demand")
    private Integer maxDailyDemand;

    /**
     * Demand range (max - min).
     */
    @JsonProperty("demand_range")
    private Integer demandRange;

    /**
     * Coefficient of Variation: σ / μ
     * Indicates demand stability:
     * - CV < 0.5: Stable demand
     * - 0.5 ≤ CV < 1.0: Moderate variability
     * - CV ≥ 1.0: High variability
     */
    @JsonProperty("coefficient_of_variation")
    private Double coefficientOfVariation;

    /**
     * Demand stability assessment.
     */
    @JsonProperty("demand_stability")
    private String demandStability;

    /**
     * Indicates if demand is stable (CV < 0.5).
     */
    @JsonProperty("is_stable_demand")
    private Boolean isStableDemand;

    /**
     * Indicates if demand is highly variable (CV ≥ 1.0).
     */
    @JsonProperty("is_high_variability_demand")
    private Boolean isHighVariabilityDemand;
}
