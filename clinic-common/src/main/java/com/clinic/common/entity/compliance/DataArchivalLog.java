package com.clinic.common.entity.compliance;

import com.clinic.common.entity.BaseEntity;
import com.clinic.common.enums.EntityType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Archival Log - Tracks execution of data retention policies
 *
 * Purpose: Audit trail for automated data archival/retention operations
 * One record per policy execution (daily, per entity type)
 *
 * Tracking Metrics:
 * - Execution start/end times
 * - Records processed, archived, failed
 * - Success/failure status
 * - Error messages for troubleshooting
 *
 * Invariants:
 * 1. execution_date IS NOT NULL
 * 2. records_processed >= 0
 * 3. records_archived <= records_processed (cannot archive more than processed)
 * 4. records_failed >= 0
 * 5. records_processed = records_archived + records_failed (completeness)
 * 6. end_time >= start_time (temporal ordering)
 * 7. status in {RUNNING, COMPLETED, FAILED}
 *
 * ISO 27001 Alignment:
 * - A.18.1.3: Records management and data retention compliance evidence
 * - Provides proof that policies are being executed automatically
 */
@Entity
@Table(name = "data_archival_log", indexes = {
    @Index(name = "idx_archival_log_tenant", columnList = "tenant_id"),
    @Index(name = "idx_archival_log_execution_date", columnList = "execution_date DESC"),
    @Index(name = "idx_archival_log_status", columnList = "status"),
    @Index(name = "idx_archival_log_policy", columnList = "policy_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataArchivalLog extends BaseEntity {

    /**
     * Tenant owning this archival log
     * Invariant: NOT NULL (every archival belongs to a tenant)
     */
    @Column(name = "tenant_id", nullable = false)
    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    /**
     * Reference to the policy that was executed
     * Foreign key to DataRetentionPolicy
     * Invariant: NOT NULL
     */
    @Column(name = "policy_id", nullable = false)
    @NotNull(message = "Policy ID is required")
    private UUID policyId;

    /**
     * Date the archival was executed (not timestamp, just date)
     * Invariant: NOT NULL
     */
    @Column(name = "execution_date", nullable = false)
    @NotNull(message = "Execution date is required")
    private LocalDate executionDate;

    /**
     * Type of entity archived
     * Invariant: NOT NULL
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    @NotNull(message = "Entity type is required")
    private EntityType entityType;

    /**
     * Number of records examined by the policy
     * Invariant: >= 0
     */
    @Column(name = "records_processed")
    @Min(value = 0, message = "Records processed cannot be negative")
    @Builder.Default
    private Long recordsProcessed = 0L;

    /**
     * Number of records successfully archived
     * Invariant: >= 0 AND records_archived <= records_processed
     */
    @Column(name = "records_archived")
    @Min(value = 0, message = "Records archived cannot be negative")
    @Builder.Default
    private Long recordsArchived = 0L;

    /**
     * Number of records that failed to archive
     * Invariant: >= 0
     */
    @Column(name = "records_failed")
    @Min(value = 0, message = "Records failed cannot be negative")
    @Builder.Default
    private Long recordsFailed = 0L;

    /**
     * Execution start time
     * Invariant: NOT NULL, precision to millisecond
     */
    @Column(name = "start_time", nullable = false)
    @NotNull(message = "Start time is required")
    private Instant startTime;

    /**
     * Execution end time
     * Invariant: end_time >= start_time (no time travel)
     * NULL if execution is still RUNNING
     */
    @Column(name = "end_time")
    private Instant endTime;

    /**
     * Execution duration in seconds
     * Calculated as: (end_time - start_time).seconds
     * Invariant: >= 0, NULL if still running
     */
    @Column(name = "duration_seconds")
    @Min(value = 0, message = "Duration cannot be negative")
    private Integer durationSeconds;

    /**
     * Status of the archival execution
     * Invariant: NOT NULL, in {RUNNING, COMPLETED, FAILED}
     * Transitions: RUNNING → COMPLETED or RUNNING → FAILED
     */
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Status is required")
    private String status; // RUNNING, COMPLETED, FAILED

    /**
     * Error message if execution failed
     * Invariant: NOT NULL only if status = FAILED
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Timestamp when this log entry was created
     * Used to track when archival process ran
     */
    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required")
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Validate archival log invariants
     * Discrete Math: Enforce via @PrePersist/@PreUpdate
     */
    @PrePersist
    @PreUpdate
    protected void validateInvariants() {
        // Invariant 3: records_archived <= records_processed
        if (recordsArchived != null && recordsProcessed != null &&
            recordsArchived > recordsProcessed) {
            throw new IllegalStateException(
                "Invariant violation: records_archived (" + recordsArchived +
                ") > records_processed (" + recordsProcessed + ")"
            );
        }

        // Invariant 5: records_processed = records_archived + records_failed
        if (recordsProcessed != null && recordsArchived != null && recordsFailed != null) {
            long total = recordsArchived + recordsFailed;
            if (total != recordsProcessed) {
                throw new IllegalStateException(
                    "Invariant violation: records_processed (" + recordsProcessed +
                    ") != records_archived (" + recordsArchived +
                    ") + records_failed (" + recordsFailed + ")"
                );
            }
        }

        // Invariant 6: end_time >= start_time
        if (endTime != null && startTime != null &&
            endTime.isBefore(startTime)) {
            throw new IllegalStateException(
                "Invariant violation: end_time before start_time"
            );
        }

        // Invariant 7: status validation
        if (status != null &&
            !status.equals("RUNNING") &&
            !status.equals("COMPLETED") &&
            !status.equals("FAILED")) {
            throw new IllegalStateException(
                "Invariant violation: Invalid status: " + status
            );
        }
    }

    /**
     * Ensure created_at is set on creation
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
