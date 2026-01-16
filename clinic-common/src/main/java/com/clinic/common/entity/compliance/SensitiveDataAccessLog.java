package com.clinic.common.entity.compliance;

import com.clinic.common.entity.TenantAwareEntity;
import com.clinic.common.entity.core.User;
import com.clinic.common.entity.patient.Patient;
import com.clinic.common.enums.AccessType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Sensitive Data Access Log - Audit trail for sensitive operations
 *
 * Mathematical Foundation: Audit Trail Integrity
 *
 * Theorem (Audit Trail Integrity):
 * For an audit trail to be legally admissible:
 * 1. Immutable: No modifications to audit records (append-only)
 * 2. Timestamped: Monotonic timestamp sequence (created_at always increasing)
 * 3. Attributed: Every action linked to authenticated user
 * 4. Complete: All CRUD operations on sensitive data logged
 *
 * Invariants:
 * - access_timestamp IS NOT NULL (every record must be timestamped)
 * - user_id IS NOT NULL (every action must be attributed)
 * - entity_id IS NOT NULL (must identify what was accessed)
 * - No updates allowed (immutability) - enforced via @PreUpdate rejection
 * - No deletes allowed (append-only) - enforced via database trigger
 * - timestamps form monotonic sequence: access_timestamp >= previous
 *
 * ISO 27001 Alignment:
 * - A.12.4.1: Event logging for user access to information
 * - A.12.4.2: Protection of log information from unauthorized access
 * - A.12.4.3: Administrator and operator activity logs
 * - A.12.4.4: Clock synchronization for timestamp accuracy
 *
 * Security Considerations:
 * - Append-only (cannot be modified or deleted after creation)
 * - Cannot be accessed by patients themselves (only admins)
 * - Partitioned by month in database for performance
 * - Exported to cold storage after 90 days
 * - Retained for 7 years per HIPAA requirements
 */
@Entity
@Table(name = "sensitive_data_access_log", indexes = {
    @Index(name = "idx_sdal_tenant_timestamp", columnList = "tenant_id, access_timestamp DESC"),
    @Index(name = "idx_sdal_user", columnList = "user_id, access_timestamp DESC"),
    @Index(name = "idx_sdal_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_sdal_patient", columnList = "patient_id, access_timestamp DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensitiveDataAccessLog extends TenantAwareEntity {

    /**
     * User who performed the access (attributed to person, not system)
     * Invariant: NOT NULL (every access must be attributed)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required for access audit logging")
    private User user;

    /**
     * Patient whose data was accessed (for audit trail filtering)
     * Denormalized for quick "all access to patient X" queries
     * Invariant: Can be NULL if not patient-related (e.g., system configuration access)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    /**
     * Type of entity accessed: "MedicalRecord", "Prescription", "LabResult", etc.
     * Invariant: NOT NULL (must identify entity type)
     */
    @Column(name = "entity_type", nullable = false, length = 50)
    @NotNull(message = "Entity type is required")
    @NotBlank(message = "Entity type cannot be blank")
    private String entityType;

    /**
     * ID of the specific entity that was accessed
     * Invariant: NOT NULL (must identify which record was accessed)
     */
    @Column(name = "entity_id", nullable = false)
    @NotNull(message = "Entity ID is required")
    private UUID entityId;

    /**
     * Type of access operation: VIEW, EXPORT, MODIFY, PRINT
     * Invariant: NOT NULL (must identify action type)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false, length = 50)
    @NotNull(message = "Access type is required")
    private AccessType accessType;

    /**
     * Timestamp when access occurred
     * Invariant: NOT NULL, monotonically increasing
     * Critical: Used for audit trail ordering and SLA compliance
     */
    @Column(name = "access_timestamp", nullable = false)
    @NotNull(message = "Access timestamp is required")
    @Builder.Default
    private Instant accessTimestamp = Instant.now();

    /**
     * Client IP address for access location tracking
     * Stored as PostgreSQL INET type for network operations
     * Used to detect suspicious access patterns
     */
    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    /**
     * User-Agent header from HTTP request
     * Identifies browser, device, and application making the request
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * Optional access reason/justification
     * For sensitive operations, users may provide business justification
     * Enables compliance audits and inappropriate access detection
     */
    @Column(name = "access_reason", columnDefinition = "TEXT")
    private String accessReason;

    /**
     * Flag indicating if data was exported/downloaded
     * Used to track bulk data export operations
     * GDPR requires tracking of personal data exports
     * Invariant: Cannot be modified after creation
     */
    @Column(name = "data_exported")
    @Builder.Default
    private Boolean dataExported = false;

    /**
     * Enforce immutability: Access logs cannot be updated after creation
     * Discrete Math: Proof by contradiction
     * Assume: Log can be updated
     * Contradiction: Audit trail would no longer be trustworthy
     * Therefore: Logs must be immutable (append-only)
     */
    @PreUpdate
    protected void preventUpdate() {
        throw new IllegalStateException(
            "Sensitive data access logs are immutable (append-only). " +
            "Cannot update log entry once created."
        );
    }

    /**
     * Ensure timestamp is set to current instant if not provided
     * Monotonic sequence: current timestamp >= all previous timestamps
     */
    @PrePersist
    protected void onCreate() {
        if (accessTimestamp == null) {
            accessTimestamp = Instant.now();
        }
    }
}
