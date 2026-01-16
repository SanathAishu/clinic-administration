package com.clinic.backend.dto.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.clinic.common.enums.ABCClassification;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for ABC Analysis classification result.
 *
 * Represents an inventory item's classification based on Pareto Analysis.
 * Items are classified by cumulative annual value percentage:
 * - A: Cumulative value ≤ 70% (~20% of items, ~70% of value)
 * - B: Cumulative value 70-90% (~30% of items, ~20% of value)
 * - C: Cumulative value > 90% (~50% of items, ~10% of value)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ABCAnalysisDTO {

    /**
     * Inventory item ID.
     */
    @JsonProperty("item_id")
    private UUID itemId;

    /**
     * Item name/code.
     */
    @JsonProperty("item_name")
    private String itemName;

    /**
     * Annual demand in units/year.
     */
    @JsonProperty("annual_demand")
    private Double annualDemand;

    /**
     * Unit price.
     */
    @JsonProperty("unit_price")
    private BigDecimal unitPrice;

    /**
     * Total annual value: annualDemand * unitPrice
     */
    @JsonProperty("annual_value")
    private BigDecimal annualValue;

    /**
     * Cumulative annual value across all items (for ranking context).
     */
    @JsonProperty("cumulative_value")
    private BigDecimal cumulativeValue;

    /**
     * Cumulative percentage of total inventory value.
     * When this exceeds:
     * - 70%: Item transitions from A to B classification
     * - 90%: Item transitions from B to C classification
     */
    @JsonProperty("cumulative_percentage")
    private Double cumulativePercentage;

    /**
     * Item rank by value (1 = highest value).
     */
    @JsonProperty("rank")
    private Integer rank;

    /**
     * ABC Classification for this item.
     * A = High value, tight control required
     * B = Medium value, moderate control
     * C = Low value, loose control acceptable
     */
    @JsonProperty("classification")
    private ABCClassification classification;

    /**
     * Recommended control strategy for this classification.
     */
    @JsonProperty("recommended_control_strategy")
    private String recommendedControlStrategy;

    /**
     * Recommended review frequency for this classification.
     */
    @JsonProperty("recommended_review_frequency")
    private String recommendedReviewFrequency;

    /**
     * Recommended service level (no-stockout probability).
     * A items: 95-99% (0.95-0.99)
     * B items: 90% (0.90)
     * C items: 75-80% (0.75-0.80)
     */
    @JsonProperty("recommended_service_level")
    private Double recommendedServiceLevel;

    /**
     * Current ABC classification before this analysis (for change tracking).
     */
    @JsonProperty("previous_classification")
    private ABCClassification previousClassification;

    /**
     * Indicates if classification changed in this analysis.
     */
    @JsonProperty("classification_changed")
    private Boolean classificationChanged;
}
