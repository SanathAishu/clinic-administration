package com.clinic.common.entity.compliance;

import com.clinic.common.entity.TenantAwareEntity;
import com.clinic.common.enums.ArchivalAction;
import com.clinic.common.enums.EntityType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;

/**
 * Data Retention Policy Entity - Configurable data lifecycle management
 *
 * Mathematical Foundation: Data Lifecycle Management
 *
 * Retention Policy Formula:
 * Archive Date = Created Date + Retention Period
 * Delete Date = Archive Date + Grace Period
 *
 * Storage Cost Optimization:
 * Total Cost = (Hot Storage Cost × Hot Data Volume) + (Cold Storage Cost × Cold Data Volume)
 *
 * Theorem 1 (Pareto Principle for Data):
 * 80% of queries access 20% of data (recent records).
 * Archival of old data reduces hot storage costs without impacting performance.
 * ROI: Archiving 80% of old data saves 75%+ of storage costs
 *
 * Invariants (Discrete Math):
 * 1. retention_days >= 1 (minimum 1 day)
 * 2. grace_period_days >= 0 (grace period is optional)
 * 3. retention_days + grace_period_days = total_data_lifecycle
 * 4. One unique policy per tenant + entity type combination
 * 5. enabled is boolean (TRUE or FALSE, never NULL)
 *
 * ISO 27001 Alignment:
 * - A.18.1.3: Protection of records (retention and disposal)
 * - A.18.1.4: Privacy and protection of personal information
 * - Automated enforcement of data lifecycle policies
 */
@Entity
@Table(name = "data_retention_policies",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_retention_policy_tenant_entity",
            columnNames = {"tenant_id", "entity_type"}
        )
    },
    indexes = {
        @Index(name = "idx_retention_policies_tenant", columnList = "tenant_id"),
        @Index(name = "idx_retention_policies_enabled", columnList = "tenant_id, enabled")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataRetentionPolicy extends TenantAwareEntity {

    /**
     * Type of entity this policy applies to (AUDIT_LOG, PATIENT_RECORD, etc.)
     * Invariant: NOT NULL, unique per tenant
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    @NotNull(message = "Entity type is required")
    private EntityType entityType;

    /**
     * Number of days to retain data before archival/deletion
     * Invariant: >= 1 (minimum 1 day retention)
     * Examples:
     * - AUDIT_LOG: 2555 days (7 years)
     * - PATIENT_RECORD: 2555 days (7 years)
     * - SESSION: 90 days
     * - NOTIFICATION: 30 days
     */
    @Column(name = "retention_days", nullable = false)
    @NotNull(message = "Retention days is required")
    @Min(value = 1, message = "Retention period must be at least 1 day")
    @Max(value = 36500, message = "Retention period cannot exceed 100 years")
    private Integer retentionDays;

    /**
     * Grace period after retention expires before hard deletion
     * Invariant: >= 0 (optional, but cannot be negative)
     * Purpose: Allow recovery/audit of old records before final deletion
     * Example: 30-day grace period for consent records before anonymization
     */
    @Column(name = "grace_period_days", nullable = false)
    @NotNull(message = "Grace period days is required")
    @Min(value = 0, message = "Grace period cannot be negative")
    @Max(value = 365, message = "Grace period cannot exceed 1 year")
    @Builder.Default
    private Integer gracePeriodDays = 30;

    /**
     * Action to take when retention expires
     * Invariant: NOT NULL
     * Options:
     * - SOFT_DELETE: Mark as deleted, keep in hot storage
     * - EXPORT_TO_S3: Archive to cold storage, remove from database
     * - ANONYMIZE: Replace PII with pseudonymous values
     * - HARD_DELETE: Permanently remove from database (rarely used)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "archival_action", nullable = false, length = 50)
    @NotNull(message = "Archival action is required")
    @Builder.Default
    private ArchivalAction archivalAction = ArchivalAction.SOFT_DELETE;

    /**
     * Is this policy currently active?
     * Invariant: NOT NULL, default TRUE
     * Disabled policies are not executed by the scheduler
     */
    @Column(name = "enabled", nullable = false)
    @NotNull(message = "Enabled flag is required")
    @Builder.Default
    private Boolean enabled = true;

    /**
     * Timestamp of last policy execution
     * Updated automatically by DataRetentionService
     * Used to prevent duplicate execution and monitor policy health
     */
    @Column(name = "last_execution")
    private Instant lastExecution;

    /**
     * Cumulative count of records archived under this policy
     * Updated after each successful execution
     * Used for compliance reporting and storage cost analysis
     */
    @Column(name = "records_archived")
    @Min(value = 0, message = "Records archived cannot be negative")
    @Builder.Default
    private Long recordsArchived = 0L;

    /**
     * Validate policy constraints
     * Discrete Math: Enforce invariants via @PrePersist/@PreUpdate
     *
     * Invariants validated:
     * 1. retention_days >= 1
     * 2. grace_period_days >= 0
     * 3. entity_type is not null
     * 4. archival_action is not null
     */
    @PrePersist
    @PreUpdate
    protected void validatePolicy() {
        if (retentionDays == null || retentionDays < 1) {
            throw new IllegalStateException(
                "Invariant violation: Retention period must be at least 1 day. Current: " + retentionDays
            );
        }

        if (gracePeriodDays == null || gracePeriodDays < 0) {
            throw new IllegalStateException(
                "Invariant violation: Grace period cannot be negative. Current: " + gracePeriodDays
            );
        }

        if (entityType == null) {
            throw new IllegalStateException(
                "Invariant violation: Entity type is required"
            );
        }

        if (archivalAction == null) {
            throw new IllegalStateException(
                "Invariant violation: Archival action is required"
            );
        }
    }
}
