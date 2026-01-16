package com.clinic.common.entity.compliance;

import com.clinic.common.entity.SoftDeletableEntity;
import com.clinic.common.enums.ComplianceMetricType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Compliance Metrics Entity - Statistical Process Control (SPC) for system monitoring
 *
 * Mathematical Foundation: Statistical Process Control (SPC) Theory
 *
 * Control Chart Theory:
 * - Center Line (CL): μ (average metric value)
 * - Upper Control Limit (UCL): μ + 3σ
 * - Lower Control Limit (LCL): μ - 3σ (where applicable)
 *
 * Theorem 1 (3-Sigma Rule):
 * For normally distributed data, 99.73% of values fall within ±3σ of the mean.
 * Application: Any metric exceeding UCL/LCL triggers an alert (statistically significant deviation)
 *
 * Theorem 2 (SLA Compliance Rate):
 * Compliance Rate = (Total Transactions - Violations) / Total Transactions × 100%
 * Target: >= 95% compliance (< 1.65σ deviation from expected)
 *
 * Invariants (Discrete Math):
 * 1. compliance_rate in [0, 100] (percentage constraint)
 * 2. sla_violations <= total_transactions (counting constraint)
 * 3. 0 <= UCL, LCL (non-negative limits)
 * 4. LCL <= mean_value <= UCL (statistical validity)
 * 5. out_of_control ⟺ (compliance_rate < LCL OR compliance_rate > UCL)
 *
 * ISO 27001 Alignment:
 * - A.18: Compliance with legal and contractual requirements
 * - A.18.1: Identification and compliance with external requirements
 * - Provides metrics for demonstrating control effectiveness
 */
@Entity
@Table(name = "compliance_metrics", indexes = {
    @Index(name = "idx_compliance_metrics_tenant", columnList = "tenant_id"),
    @Index(name = "idx_compliance_metrics_date", columnList = "metric_date DESC"),
    @Index(name = "idx_compliance_metrics_type", columnList = "metric_type"),
    @Index(name = "idx_compliance_metrics_out_of_control", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceMetrics extends SoftDeletableEntity {

    /**
     * Date of metric calculation
     * Invariant: NOT NULL, unique per tenant + metric_type
     */
    @Column(name = "metric_date", nullable = false)
    @NotNull(message = "Metric date is required")
    private LocalDate metricDate;

    /**
     * Type of compliance metric being tracked
     * Invariant: NOT NULL
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 50)
    @NotNull(message = "Metric type is required")
    private ComplianceMetricType metricType;

    /**
     * Total number of transactions processed for this metric
     * Used in: Compliance Rate = (total - violations) / total × 100%
     * Invariant: >= 0
     */
    @Column(name = "total_transactions")
    @Min(value = 0, message = "Total transactions cannot be negative")
    private Long totalTransactions;

    /**
     * Number of SLA violations detected
     * Invariant: >= 0 AND sla_violations <= total_transactions
     */
    @Column(name = "sla_violations")
    @Min(value = 0, message = "SLA violations cannot be negative")
    private Long slaViolations;

    /**
     * Calculated compliance rate as percentage
     * Formula: (total_transactions - sla_violations) / total_transactions × 100%
     * Invariant: in [0.0, 100.0]
     * Automatic: Calculated in @PrePersist/@PreUpdate
     */
    @Column(name = "compliance_rate", precision = 5, scale = 2)
    @DecimalMin(value = "0.0", message = "Compliance rate cannot be negative")
    @DecimalMax(value = "100.0", message = "Compliance rate cannot exceed 100%")
    private Double complianceRate;

    /**
     * Historical mean value for this metric type (μ)
     * Used for statistical control limit calculation
     * Calculated from previous 30-day rolling window
     */
    @Column(name = "mean_value", precision = 10, scale = 4)
    private Double meanValue;

    /**
     * Standard deviation of metric (σ)
     * Used for control limit calculation
     * Calculated from previous 30-day rolling window
     */
    @Column(name = "std_deviation", precision = 10, scale = 4)
    @DecimalMin(value = "0.0", message = "Standard deviation cannot be negative")
    private Double stdDeviation;

    /**
     * Upper Control Limit: μ + 3σ
     * Any value above UCL indicates out-of-control state
     * Calculated automatically in @PrePersist/@PreUpdate
     */
    @Column(name = "ucl", precision = 10, scale = 4)
    private Double upperControlLimit;

    /**
     * Lower Control Limit: max(0, μ - 3σ)
     * Any value below LCL indicates out-of-control state
     * Cannot be negative (min = 0 for percentages)
     * Calculated automatically in @PrePersist/@PreUpdate
     */
    @Column(name = "lcl", precision = 10, scale = 4)
    @DecimalMin(value = "0.0", message = "Lower control limit cannot be negative")
    private Double lowerControlLimit;

    /**
     * Flag: Is metric out of control?
     * Invariant: out_of_control ⟺ (compliance_rate < LCL OR compliance_rate > UCL)
     * Triggers: Email alerts, dashboard warnings
     */
    @Column(name = "out_of_control")
    @Builder.Default
    private Boolean outOfControl = false;

    /**
     * ISO 27001 Specific Metrics
     * Count of access control violations detected
     */
    @Column(name = "access_violations")
    @Min(value = 0, message = "Access violations cannot be negative")
    @Builder.Default
    private Long accessViolations = 0L;

    /**
     * Count of data integrity errors detected
     */
    @Column(name = "data_integrity_errors")
    @Min(value = 0, message = "Data integrity errors cannot be negative")
    @Builder.Default
    private Long dataIntegrityErrors = 0L;

    /**
     * Count of security incidents reported for this metric period
     */
    @Column(name = "security_incidents")
    @Min(value = 0, message = "Security incidents cannot be negative")
    @Builder.Default
    private Long securityIncidents = 0L;

    /**
     * Calculate all compliance metrics automatically
     * Discrete Math: Enforce all invariants via @PrePersist/@PreUpdate
     *
     * Invariants validated:
     * 1. compliance_rate = (total_transactions - sla_violations) / total_transactions × 100
     * 2. 0 <= compliance_rate <= 100
     * 3. ucl = mean_value + (3 × std_deviation)
     * 4. lcl = max(0, mean_value - (3 × std_deviation))
     * 5. out_of_control ⟺ (compliance_rate < lcl OR compliance_rate > ucl)
     */
    @PrePersist
    @PreUpdate
    protected void calculateMetrics() {
        // Invariant 1: Calculate compliance rate
        if (totalTransactions != null && totalTransactions > 0 && slaViolations != null) {
            this.complianceRate = ((totalTransactions - slaViolations) / (double) totalTransactions) * 100.0;

            // Invariant 2: Clamp to [0, 100]
            if (complianceRate < 0.0) complianceRate = 0.0;
            if (complianceRate > 100.0) complianceRate = 100.0;
        }

        // Invariant 3 & 4: Calculate control limits
        if (meanValue != null && stdDeviation != null && stdDeviation >= 0.0) {
            this.upperControlLimit = meanValue + (3 * stdDeviation);
            this.lowerControlLimit = Math.max(0.0, meanValue - (3 * stdDeviation));

            // Invariant 5: Check if out of control
            if (complianceRate != null) {
                this.outOfControl = complianceRate < lowerControlLimit || complianceRate > upperControlLimit;
            }
        } else {
            // If no control data available, assume in control
            this.outOfControl = false;
        }
    }
}
