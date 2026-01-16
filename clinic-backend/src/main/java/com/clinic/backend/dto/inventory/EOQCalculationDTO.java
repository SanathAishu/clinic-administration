package com.clinic.backend.dto.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for Economic Order Quantity (EOQ) calculation response.
 *
 * Contains the calculated optimal order quantity and related metrics.
 * Formula: Q* = sqrt(2DS/H)
 *
 * Where:
 * - D = Annual demand (units/year)
 * - S = Ordering cost per order (currency)
 * - H = Holding cost per unit per year (currency/unit/year)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EOQCalculationDTO {

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
     * Ordering cost per order in currency units.
     */
    @JsonProperty("ordering_cost")
    private BigDecimal orderingCost;

    /**
     * Holding cost per unit per year.
     */
    @JsonProperty("holding_cost")
    private BigDecimal holdingCost;

    /**
     * Calculated Economic Order Quantity (units).
     * This is the optimal order quantity that minimizes total cost.
     */
    @JsonProperty("eoq")
    private Double economicOrderQuantity;

    /**
     * Number of orders per year: D / Q*
     */
    @JsonProperty("orders_per_year")
    private Double ordersPerYear;

    /**
     * Average inventory level: Q* / 2
     */
    @JsonProperty("average_inventory")
    private Double averageInventory;

    /**
     * Total annual ordering cost: (D divided by Q-star) multiplied by S
     */
    @JsonProperty("annual_ordering_cost")
    private BigDecimal annualOrderingCost;

    /**
     * Total annual holding cost: (Q-star divided by 2) multiplied by H
     */
    @JsonProperty("annual_holding_cost")
    private BigDecimal annualHoldingCost;

    /**
     * Total inventory cost (ordering plus holding).
     * At EOQ, ordering cost equals holding cost.
     */
    @JsonProperty("total_inventory_cost")
    private BigDecimal totalInventoryCost;

    /**
     * Current stock level (for reference).
     */
    @JsonProperty("current_stock")
    private Integer currentStock;

    /**
     * Recommended order frequency in days.
     */
    @JsonProperty("recommended_order_frequency_days")
    private Double recommendedOrderFrequencyDays;
}
