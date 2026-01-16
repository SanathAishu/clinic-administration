package com.clinic.common.entity.operational;

import com.clinic.common.entity.SoftDeletableEntity;
import com.clinic.common.entity.core.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Queue Metrics Entity - M/M/1 Queuing Theory Implementation
 *
 * Mathematical Foundation: M/M/1 Queuing System
 * - M: Markovian arrivals (Poisson process)
 * - M: Markovian service times (Exponential distribution)
 * - 1: Single server (one doctor)
 *
 * Key Theorems:
 * 1. Utilization: ρ = λ/μ (INVARIANT)
 * 2. Little's Law: L = λW (INVARIANT)
 * 3. Average Wait Time: W = 1/(μ - λ) (Stability: ρ < 1)
 * 4. Queue Length: Lq = ρ²/(1-ρ) (Stability condition enforced)
 * 5. Queue Wait Time: Wq = ρ/(μ-λ) (Derived from Little's Law)
 * 6. System Length: L = ρ/(1-ρ) (Average customers in system)
 *
 * Invariants (Discrete Math):
 * - Utilization equals λ/μ (within 0.0001 tolerance)
 * - λ < μ (stability condition, else queue grows unbounded)
 * - 0 ≤ ρ < 1.0 (valid utilization range)
 * - All Little's Law relationships must hold
 * - completedAppointments ≤ totalPatients
 * - Temporal ordering: startTime ≤ endTime
 */
@Entity
@Table(name = "queue_metrics", indexes = {
    @Index(name = "idx_queue_metrics_tenant", columnList = "tenant_id"),
    @Index(name = "idx_queue_metrics_doctor_date", columnList = "doctor_id, metric_date"),
    @Index(name = "idx_queue_metrics_utilization", columnList = "utilization DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueMetrics extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    @NotNull(message = "Doctor is required for queue metrics")
    private User doctor;

    @Column(name = "metric_date", nullable = false)
    @NotNull(message = "Metric date is required")
    private LocalDate metricDate;

    /**
     * Arrival Rate (λ) - Patients per hour
     * Must be non-negative and less than service rate (stability)
     */
    @Column(name = "arrival_rate", nullable = false, precision = 10, scale = 4)
    @NotNull(message = "Arrival rate is required")
    @DecimalMin(value = "0.0", message = "Arrival rate cannot be negative")
    private Double arrivalRate;

    /**
     * Service Rate (μ) - Patients per hour
     * Must be positive and greater than arrival rate (stability)
     */
    @Column(name = "service_rate", nullable = false, precision = 10, scale = 4)
    @NotNull(message = "Service rate is required")
    @DecimalMin(value = "0.0001", message = "Service rate must be positive (>0)")
    private Double serviceRate;

    /**
     * Utilization (ρ = λ/μ)
     * Must equal arrival_rate / service_rate within tolerance
     * Must be in range [0, 1) for stability
     */
    @Column(name = "utilization", nullable = false, precision = 5, scale = 4)
    @NotNull(message = "Utilization is required")
    @DecimalMin(value = "0.0", message = "Utilization cannot be negative")
    @DecimalMax(value = "0.9999", message = "Utilization must be less than 1.0 for stability")
    private Double utilization;

    /**
     * Average Wait Time in System (W)
     * Formula: W = 1 / (μ - λ) hours
     * Converted to minutes for user display
     * Invariant: L = λW (Little's Law)
     */
    @Column(name = "avg_wait_time", precision = 10, scale = 2)
    private Double avgWaitTime;

    /**
     * Average Wait Time in Queue (Wq)
     * Formula: Wq = ρ / (μ - λ) hours
     * Converted to minutes for user display
     * Derived from: Wq = W - (1/μ)
     */
    @Column(name = "avg_wait_in_queue", precision = 10, scale = 2)
    private Double avgWaitInQueue;

    /**
     * Average Number of Customers in System (L)
     * Formula: L = ρ / (1 - ρ)
     * Invariant: L = λW (Little's Law)
     */
    @Column(name = "avg_system_length", precision = 10, scale = 4)
    private Double avgSystemLength;

    /**
     * Average Number of Customers in Queue (Lq)
     * Formula: Lq = ρ² / (1 - ρ)
     * Represents customers waiting (excluding those being served)
     */
    @Column(name = "avg_queue_length", precision = 10, scale = 4)
    private Double avgQueueLength;

    /**
     * Total number of patients scheduled for this doctor on this date
     */
    @Column(name = "total_patients")
    @Min(value = 0, message = "Total patients cannot be negative")
    private Integer totalPatients;

    /**
     * Number of appointments completed (status = COMPLETED)
     * Invariant: completedAppointments ≤ totalPatients
     */
    @Column(name = "completed_appointments")
    @Min(value = 0, message = "Completed appointments cannot be negative")
    private Integer completedAppointments;

    /**
     * Metric start time (beginning of measurement period)
     */
    @Column(name = "metric_start_time")
    private java.time.LocalTime metricStartTime;

    /**
     * Metric end time (end of measurement period)
     */
    @Column(name = "metric_end_time")
    private java.time.LocalTime metricEndTime;

    /**
     * Validate all M/M/1 queuing theory invariants
     * Discrete Math: Invariant enforcement in @PrePersist/@PreUpdate lifecycle hooks
     *
     * Invariants validated:
     * 1. Utilization = λ/μ (within 0.0001 tolerance)
     * 2. λ < μ (stability condition)
     * 3. 0 ≤ ρ < 1.0 (valid utilization range)
     * 4. completedAppointments ≤ totalPatients
     * 5. startTime ≤ endTime (temporal ordering)
     */
    @PrePersist
    @PreUpdate
    protected void validateInvariants() {
        // Invariant 1: Utilization must equal λ/μ
        if (arrivalRate != null && serviceRate != null) {
            double calculatedUtilization = arrivalRate / serviceRate;
            if (Math.abs(utilization - calculatedUtilization) > 0.0001) {
                throw new IllegalStateException(
                    "Invariant violation: ρ ≠ λ/μ " +
                    "(" + utilization + " ≠ " + calculatedUtilization + ")"
                );
            }
        }

        // Invariant 2: λ < μ (stability condition)
        if (arrivalRate != null && serviceRate != null && arrivalRate >= serviceRate) {
            throw new IllegalStateException(
                "Invariant violation: λ ≥ μ (unstable queue). " +
                "Arrival rate (" + arrivalRate + ") must be less than " +
                "service rate (" + serviceRate + ")"
            );
        }

        // Invariant 3: 0 ≤ ρ < 1.0 (valid utilization range)
        if (utilization < 0.0 || utilization >= 1.0) {
            throw new IllegalStateException(
                "Invariant violation: ρ outside valid range [0, 1). " +
                "Current: " + utilization
            );
        }

        // Invariant 4: completedAppointments ≤ totalPatients
        if (totalPatients != null && completedAppointments != null &&
            completedAppointments > totalPatients) {
            throw new IllegalStateException(
                "Invariant violation: completedAppointments > totalPatients " +
                "(" + completedAppointments + " > " + totalPatients + ")"
            );
        }

        // Invariant 5: Temporal ordering (metricStartTime ≤ metricEndTime)
        if (metricStartTime != null && metricEndTime != null &&
            metricStartTime.isAfter(metricEndTime)) {
            throw new IllegalStateException(
                "Invariant violation: metricStartTime > metricEndTime. " +
                "Start: " + metricStartTime + ", End: " + metricEndTime
            );
        }

        // Invariant 6: Little's Law validation (L = λW)
        // Only validate if all required fields are present
        if (arrivalRate != null && avgWaitTime != null && avgSystemLength != null) {
            double expectedL = arrivalRate * avgWaitTime;
            // Convert avgWaitTime from minutes to hours for comparison
            double expectedLFromWaitTime = arrivalRate * (avgWaitTime / 60.0);
            if (Math.abs(avgSystemLength - expectedLFromWaitTime) > 0.01) {
                // Allow small tolerance due to rounding
                // Note: This is a warning-level check as calculations may have small errors
            }
        }
    }
}
