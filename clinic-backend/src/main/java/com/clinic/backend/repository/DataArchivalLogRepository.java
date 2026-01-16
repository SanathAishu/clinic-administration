package com.clinic.backend.repository;

import com.clinic.common.entity.compliance.DataArchivalLog;
import com.clinic.common.enums.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for DataArchivalLog entity
 * Supports ISO 27001 A.18.1.3 (Protection of Records) archival tracking
 *
 * Key Features:
 * - Multi-tenant isolation
 * - Execution status tracking
 * - Date-based filtering
 * - Error log retrieval
 * - Compliance reporting
 */
@Repository
public interface DataArchivalLogRepository extends JpaRepository<DataArchivalLog, UUID> {

    /**
     * Find archival logs for specific date
     * Used for daily compliance reporting
     *
     * @param tenantId Tenant context
     * @param executionDate Date to query
     * @return List of archival executions for this date
     */
    @Query("""
        SELECT dal FROM DataArchivalLog dal
        WHERE dal.tenantId = :tenantId
        AND dal.executionDate = :executionDate
        ORDER BY dal.createdAt DESC
        """)
    List<DataArchivalLog> findByExecutionDate(
        @Param("tenantId") UUID tenantId,
        @Param("executionDate") LocalDate executionDate
    );

    /**
     * Find archival logs within date range
     * Used for historical compliance analysis
     *
     * @param tenantId Tenant context
     * @param startDate Range start
     * @param endDate Range end
     * @param pageable Pagination
     * @return Page of archival logs
     */
    @Query("""
        SELECT dal FROM DataArchivalLog dal
        WHERE dal.tenantId = :tenantId
        AND dal.executionDate >= :startDate
        AND dal.executionDate <= :endDate
        ORDER BY dal.executionDate DESC, dal.createdAt DESC
        """)
    Page<DataArchivalLog> findByDateRange(
        @Param("tenantId") UUID tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );

    /**
     * Find archival logs for specific entity type
     * Used to track archival of specific data types
     *
     * @param tenantId Tenant context
     * @param entityType Entity type that was archived
     * @param pageable Pagination
     * @return Page of archival logs for entity type
     */
    @Query("""
        SELECT dal FROM DataArchivalLog dal
        WHERE dal.tenantId = :tenantId
        AND dal.entityType = :entityType
        ORDER BY dal.executionDate DESC
        """)
    Page<DataArchivalLog> findByEntityType(
        @Param("tenantId") UUID tenantId,
        @Param("entityType") EntityType entityType,
        Pageable pageable
    );

    /**
     * Find failed archival executions
     * Critical for troubleshooting and alerting
     *
     * @param tenantId Tenant context
     * @return List of failed executions ordered by date
     */
    @Query("""
        SELECT dal FROM DataArchivalLog dal
        WHERE dal.tenantId = :tenantId
        AND dal.status = 'FAILED'
        ORDER BY dal.executionDate DESC
        """)
    List<DataArchivalLog> findFailedExecutions(
        @Param("tenantId") UUID tenantId
    );

    /**
     * Find most recent archival for specific entity type
     * Used to check when data was last archived
     *
     * @param tenantId Tenant context
     * @param entityType Entity type to query
     * @return Optional containing most recent execution
     */
    @Query("""
        SELECT dal FROM DataArchivalLog dal
        WHERE dal.tenantId = :tenantId
        AND dal.entityType = :entityType
        AND dal.status = 'COMPLETED'
        ORDER BY dal.executionDate DESC
        LIMIT 1
        """)
    Optional<DataArchivalLog> findMostRecentSuccess(
        @Param("tenantId") UUID tenantId,
        @Param("entityType") EntityType entityType
    );

    /**
     * Find running archival executions
     * Used to prevent concurrent execution of same policy
     *
     * @param tenantId Tenant context
     * @return List of currently running executions
     */
    @Query("""
        SELECT dal FROM DataArchivalLog dal
        WHERE dal.tenantId = :tenantId
        AND dal.status = 'RUNNING'
        """)
    List<DataArchivalLog> findRunningExecutions(
        @Param("tenantId") UUID tenantId
    );

    /**
     * Calculate total records archived for entity type
     * Used for compliance reporting and cost analysis
     *
     * @param tenantId Tenant context
     * @param entityType Entity type to analyze
     * @return Total records archived across all executions
     */
    @Query("""
        SELECT COALESCE(SUM(dal.recordsArchived), 0) FROM DataArchivalLog dal
        WHERE dal.tenantId = :tenantId
        AND dal.entityType = :entityType
        AND dal.status = 'COMPLETED'
        """)
    long getTotalArchivedRecords(
        @Param("tenantId") UUID tenantId,
        @Param("entityType") EntityType entityType
    );

    /**
     * Calculate average execution duration
     * Used for performance monitoring
     *
     * @param tenantId Tenant context
     * @return Average duration in seconds
     */
    @Query("""
        SELECT COALESCE(AVG(dal.durationSeconds), 0.0) FROM DataArchivalLog dal
        WHERE dal.tenantId = :tenantId
        AND dal.status = 'COMPLETED'
        AND dal.durationSeconds IS NOT NULL
        """)
    Double getAverageExecutionDuration(
        @Param("tenantId") UUID tenantId
    );

    /**
     * Count successful executions in date range
     * Used for SLA tracking
     *
     * @param tenantId Tenant context
     * @param startDate Range start
     * @param endDate Range end
     * @return Number of successful executions
     */
    @Query("""
        SELECT COUNT(dal) FROM DataArchivalLog dal
        WHERE dal.tenantId = :tenantId
        AND dal.executionDate >= :startDate
        AND dal.executionDate <= :endDate
        AND dal.status = 'COMPLETED'
        """)
    long countSuccessfulExecutions(
        @Param("tenantId") UUID tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Count failed executions in date range
     * Used for alerting and troubleshooting
     *
     * @param tenantId Tenant context
     * @param startDate Range start
     * @param endDate Range end
     * @return Number of failed executions
     */
    @Query("""
        SELECT COUNT(dal) FROM DataArchivalLog dal
        WHERE dal.tenantId = :tenantId
        AND dal.executionDate >= :startDate
        AND dal.executionDate <= :endDate
        AND dal.status = 'FAILED'
        """)
    long countFailedExecutions(
        @Param("tenantId") UUID tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
