package com.clinic.backend.repository;

import com.clinic.common.entity.compliance.SensitiveDataAccessLog;
import com.clinic.common.enums.AccessType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for SensitiveDataAccessLog entity
 * Supports ISO 27001 A.12.4 (Logging & Monitoring) audit trail queries
 *
 * Key Features:
 * - Multi-tenant isolation (tenant_id filtering)
 * - Timestamp-based filtering for audit windows
 * - Patient-scoped access audits
 * - User activity tracking
 * - Access type filtering for compliance reports
 */
@Repository
public interface SensitiveDataAccessLogRepository extends JpaRepository<SensitiveDataAccessLog, UUID> {

    /**
     * Find all access logs for a specific patient within date range
     * Used for patient privacy audits (HIPAA 164.308(a)(7))
     *
     * @param tenantId Tenant context
     * @param patientId Patient to audit
     * @param startTime Date range start
     * @param endTime Date range end
     * @return List of access logs ordered by timestamp descending
     */
    @Query("""
        SELECT sal FROM SensitiveDataAccessLog sal
        WHERE sal.tenantId = :tenantId
        AND sal.patient.id = :patientId
        AND sal.accessTimestamp >= :startTime
        AND sal.accessTimestamp <= :endTime
        AND sal.deletedAt IS NULL
        ORDER BY sal.accessTimestamp DESC
        """)
    List<SensitiveDataAccessLog> findPatientAccessLogs(
        @Param("tenantId") UUID tenantId,
        @Param("patientId") UUID patientId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find all access logs by a specific user
     * Used for user activity audits (who accessed what)
     *
     * @param tenantId Tenant context
     * @param userId User performing access
     * @param startTime Date range start
     * @param endTime Date range end
     * @param pageable Pagination
     * @return Page of access logs
     */
    @Query("""
        SELECT sal FROM SensitiveDataAccessLog sal
        WHERE sal.tenantId = :tenantId
        AND sal.user.id = :userId
        AND sal.accessTimestamp >= :startTime
        AND sal.accessTimestamp <= :endTime
        AND sal.deletedAt IS NULL
        ORDER BY sal.accessTimestamp DESC
        """)
    Page<SensitiveDataAccessLog> findUserAccessLogs(
        @Param("tenantId") UUID tenantId,
        @Param("userId") UUID userId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        Pageable pageable
    );

    /**
     * Find all data export operations (bulk data downloads)
     * Critical for GDPR compliance and data protection audits
     *
     * @param tenantId Tenant context
     * @param startTime Date range start
     * @param endTime Date range end
     * @return List of export operations
     */
    @Query("""
        SELECT sal FROM SensitiveDataAccessLog sal
        WHERE sal.tenantId = :tenantId
        AND sal.dataExported = true
        AND sal.accessTimestamp >= :startTime
        AND sal.accessTimestamp <= :endTime
        AND sal.deletedAt IS NULL
        ORDER BY sal.accessTimestamp DESC
        """)
    List<SensitiveDataAccessLog> findDataExportOperations(
        @Param("tenantId") UUID tenantId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find access logs for specific entity (record-level access)
     * Used to audit access to specific patient records
     *
     * @param tenantId Tenant context
     * @param entityType Type of entity (MedicalRecord, Prescription, etc.)
     * @param entityId Specific entity ID
     * @return List of all access to this entity
     */
    @Query("""
        SELECT sal FROM SensitiveDataAccessLog sal
        WHERE sal.tenantId = :tenantId
        AND sal.entityType = :entityType
        AND sal.entityId = :entityId
        AND sal.deletedAt IS NULL
        ORDER BY sal.accessTimestamp DESC
        """)
    List<SensitiveDataAccessLog> findEntityAccessLogs(
        @Param("tenantId") UUID tenantId,
        @Param("entityType") String entityType,
        @Param("entityId") UUID entityId
    );

    /**
     * Find all access logs of specific type within date range
     * Used for compliance filtering (e.g., all VIEW_MEDICAL_RECORD accesses)
     *
     * @param tenantId Tenant context
     * @param accessType Type of access to filter
     * @param startTime Date range start
     * @param endTime Date range end
     * @param pageable Pagination
     * @return Page of matching access logs
     */
    @Query("""
        SELECT sal FROM SensitiveDataAccessLog sal
        WHERE sal.tenantId = :tenantId
        AND sal.accessType = :accessType
        AND sal.accessTimestamp >= :startTime
        AND sal.accessTimestamp <= :endTime
        AND sal.deletedAt IS NULL
        ORDER BY sal.accessTimestamp DESC
        """)
    Page<SensitiveDataAccessLog> findAccessLogsByType(
        @Param("tenantId") UUID tenantId,
        @Param("accessType") AccessType accessType,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        Pageable pageable
    );

    /**
     * Count access logs by access type for compliance reporting
     * Used to calculate audit coverage metrics
     *
     * @param tenantId Tenant context
     * @param startTime Date range start
     * @param endTime Date range end
     * @return Count of access logs
     */
    @Query("""
        SELECT COUNT(sal) FROM SensitiveDataAccessLog sal
        WHERE sal.tenantId = :tenantId
        AND sal.accessTimestamp >= :startTime
        AND sal.accessTimestamp <= :endTime
        AND sal.deletedAt IS NULL
        """)
    long countAccessLogs(
        @Param("tenantId") UUID tenantId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find recent access logs (last N days)
     * Used for dashboard and alert generation
     *
     * @param tenantId Tenant context
     * @param startTime Date range start (e.g., 7 days ago)
     * @param pageable Pagination
     * @return Page of recent access logs
     */
    @Query("""
        SELECT sal FROM SensitiveDataAccessLog sal
        WHERE sal.tenantId = :tenantId
        AND sal.accessTimestamp >= :startTime
        AND sal.deletedAt IS NULL
        ORDER BY sal.accessTimestamp DESC
        """)
    Page<SensitiveDataAccessLog> findRecentAccessLogs(
        @Param("tenantId") UUID tenantId,
        @Param("startTime") Instant startTime,
        Pageable pageable
    );

    /**
     * Find suspicious access patterns (potential unauthorized access)
     * Multiple accesses by same user to different patients within short time
     *
     * @param tenantId Tenant context
     * @param userId User to check
     * @param withinMinutes Time window in minutes
     * @return List of access patterns
     */
    @Query("""
        SELECT sal FROM SensitiveDataAccessLog sal
        WHERE sal.tenantId = :tenantId
        AND sal.user.id = :userId
        AND sal.accessTimestamp >= CURRENT_TIMESTAMP - INTERVAL '1 minute' * :withinMinutes
        AND sal.deletedAt IS NULL
        GROUP BY sal.patient.id
        HAVING COUNT(DISTINCT sal.patient.id) > 1
        """)
    List<SensitiveDataAccessLog> findSuspiciousAccessPatterns(
        @Param("tenantId") UUID tenantId,
        @Param("userId") UUID userId,
        @Param("withinMinutes") int withinMinutes
    );
}
