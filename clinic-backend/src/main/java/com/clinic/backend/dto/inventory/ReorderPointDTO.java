package com.clinic.backend.dto.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.UUID;

/**
 * DTO for Reorder Point (ROP) calculation response.
 *
 * Contains the reorder point and safety stock calculations.
 * Formula: ROP = d·L + SS, where SS = z·σ·√L
 *
 * Where:
 * - d = Average daily demand (units/day)
 * - L = Lead time (days)
 * - z = Z-score for service level
 * - σ = Standard deviation of daily demand
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderPointDTO {

    /**
     * Inventory item ID.
     */
    @JsonProperty("item_id")
    private UUID itemId;

    /**
     * Item name/code for reference.
     */
    @JsonProperty("item_name")
    private String itemName;

    /**
     * Annual demand in units/year.
     */
    @JsonProperty("annual_demand")
    private Double annualDemand;

    /**
     * Average daily demand.
     * d in ROP formula: ROP = d·L + z·σ·√L
     */
    @JsonProperty("average_daily_demand")
    private Double averageDailyDemand;

    /**
     * Lead time from supplier in days.
     * L in ROP formula: ROP = d·L + z·σ·√L
     */
    @JsonProperty("lead_time_days")
    private Integer leadTimeDays;

    /**
     * Standard deviation of daily demand.
     * σ in ROP formula: ROP = d·L + z·σ·√L
     */
    @JsonProperty("demand_std_dev")
    private Double demandStdDev;

    /**
     * Target service level (0.0 to 1.0).
     * Probability of no stockout during lead time.
     * Common values: 0.90 (90%), 0.95 (95%), 0.99 (99%)
     */
    @JsonProperty("service_level")
    private Double serviceLevel;

    /**
     * Z-score for the service level.
     * Used to calculate safety stock: SS = z·σ·√L
     */
    @JsonProperty("z_score")
    private Double zScore;

    /**
     * Expected demand during lead time: d·L
     * This is the base reorder point without safety buffer.
     */
    @JsonProperty("lead_time_demand")
    private Integer leadTimeDemand;

    /**
     * Safety stock: z·σ·√L
     * Buffer inventory to prevent stockouts during demand uncertainty.
     */
    @JsonProperty("safety_stock")
    private Integer safetyStock;

    /**
     * Reorder Point: ROP = d·L + SS
     * Inventory level at which to place new order.
     */
    @JsonProperty("reorder_point")
    private Integer reorderPoint;

    /**
     * Current stock level (for reference).
     */
    @JsonProperty("current_stock")
    private Integer currentStock;

    /**
     * Units below reorder point (if negative, reorder is needed).
     * If this is <= 0, a purchase order should be created.
     */
    @JsonProperty("units_below_rop")
    private Integer unitsBelowROP;

    /**
     * Indicates if reorder is needed now.
     * true if currentStock <= reorderPoint
     */
    @JsonProperty("reorder_needed")
    private Boolean reorderNeeded;
}
