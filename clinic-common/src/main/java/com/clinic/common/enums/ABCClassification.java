package com.clinic.common.enums;

/**
 * ABC Classification for Inventory Items (Pareto Analysis).
 *
 * Based on Operations Research Principle: Pareto Principle (80/20 Rule)
 *
 * Classification Distribution:
 * - A items: ~20% of items, ~70% of value → Tight control, frequent review
 * - B items: ~30% of items, ~20% of value → Moderate control, regular review
 * - C items: ~50% of items, ~10% of value → Loose control, periodic review
 *
 * Mathematical Basis:
 * Items are classified by cumulative annual value percentage:
 * - A: Cumulative value ≤ 70%
 * - B: Cumulative value 70-90%
 * - C: Cumulative value > 90%
 *
 * Operational Implications:
 * - A items: Daily review, 95-99% service level, exact record keeping
 * - B items: Weekly review, 90% service level, standard procedures
 * - C items: Monthly review, 70-80% service level, simplified procedures
 */
public enum ABCClassification {
    /**
     * A Classification - High Value Items.
     * Top 70% of annual inventory value (~20% of items).
     * Requires intensive control and frequent monitoring.
     */
    A("High Value", 70, 0.95),

    /**
     * B Classification - Medium Value Items.
     * Next 20% of annual inventory value (~30% of items).
     * Requires moderate control and regular monitoring.
     */
    B("Medium Value", 20, 0.90),

    /**
     * C Classification - Low Value Items.
     * Bottom 10% of annual inventory value (~50% of items).
     * Requires loose control and minimal monitoring.
     */
    C("Low Value", 10, 0.75);

    private final String description;
    private final double valuePercentage;
    private final double recommendedServiceLevel;

    ABCClassification(String description, double valuePercentage, double recommendedServiceLevel) {
        this.description = description;
        this.valuePercentage = valuePercentage;
        this.recommendedServiceLevel = recommendedServiceLevel;
    }

    public String getDescription() {
        return description;
    }

    public double getValuePercentage() {
        return valuePercentage;
    }

    public double getRecommendedServiceLevel() {
        return recommendedServiceLevel;
    }
}
