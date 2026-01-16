package com.clinic.backend.repository;

import com.clinic.common.entity.compliance.DataRetentionPolicy;
import com.clinic.common.enums.ArchivalAction;
import com.clinic.common.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for DataRetentionPolicy entity
 * Supports ISO 27001 A.18.1.3 (Protection of Records) policy management
 *
 * Key Features:
 * - Multi-tenant isolation
 * - Policy lifecycle management (enable/disable)
 * - Entity type filtering
 * - Archival action categorization
 */
@Repository
public interface DataRetentionPolicyRepository extends JpaRepository<DataRetentionPolicy, UUID> {

    /**
     * Find all enabled policies for a tenant
     * Used by scheduled retention service to execute policies
     *
     * @param tenantId Tenant context
     * @return List of enabled policies ready for execution
     */
    @Query("""
        SELECT drp FROM DataRetentionPolicy drp
        WHERE drp.tenantId = :tenantId
        AND drp.enabled = true
        """)
    List<DataRetentionPolicy> findByEnabled(
        @Param("tenantId") UUID tenantId
    );

    /**
     * Find policy for specific entity type
     * Used for checking if entity has retention policy configured
     *
     * @param tenantId Tenant context
     * @param entityType Type of entity
     * @return Optional containing policy if configured
     */
    @Query("""
        SELECT drp FROM DataRetentionPolicy drp
        WHERE drp.tenantId = :tenantId
        AND drp.entityType = :entityType
        """)
    Optional<DataRetentionPolicy> findByEntityType(
        @Param("tenantId") UUID tenantId,
        @Param("entityType") EntityType entityType
    );

    /**
     * Find all policies with specific archival action
     * Used for analyzing archival strategies
     *
     * @param tenantId Tenant context
     * @param archivalAction Action to filter
     * @return List of policies using this archival action
     */
    @Query("""
        SELECT drp FROM DataRetentionPolicy drp
        WHERE drp.tenantId = :tenantId
        AND drp.archivalAction = :archivalAction
        """)
    List<DataRetentionPolicy> findByArchivalAction(
        @Param("tenantId") UUID tenantId,
        @Param("archivalAction") ArchivalAction archivalAction
    );

    /**
     * Find policies with short retention periods (risky)
     * Used for compliance audit - ensures critical data has sufficient retention
     *
     * @param tenantId Tenant context
     * @param minRetentionDays Minimum days considered safe
     * @return List of policies below threshold
     */
    @Query("""
        SELECT drp FROM DataRetentionPolicy drp
        WHERE drp.tenantId = :tenantId
        AND drp.retentionDays < :minRetentionDays
        """)
    List<DataRetentionPolicy> findByShortRetention(
        @Param("tenantId") UUID tenantId,
        @Param("minRetentionDays") int minRetentionDays
    );

    /**
     * Find policies that haven't executed recently
     * Used for health monitoring - ensure policies are actually running
     *
     * @param tenantId Tenant context
     * @return List of policies not executed in last 48 hours
     */
    @Query("""
        SELECT drp FROM DataRetentionPolicy drp
        WHERE drp.tenantId = :tenantId
        AND drp.enabled = true
        AND (drp.lastExecution IS NULL OR drp.lastExecution < CURRENT_TIMESTAMP - INTERVAL '2 days')
        """)
    List<DataRetentionPolicy> findStaleExecutions(
        @Param("tenantId") UUID tenantId
    );

    /**
     * Find all policies for a tenant
     * Used for policy management UI
     *
     * @param tenantId Tenant context
     * @return List of all policies for tenant
     */
    @Query("""
        SELECT drp FROM DataRetentionPolicy drp
        WHERE drp.tenantId = :tenantId
        ORDER BY drp.entityType
        """)
    List<DataRetentionPolicy> findAll(@Param("tenantId") UUID tenantId);

    /**
     * Check if policy exists for entity type
     * Used for validation before creating policy
     *
     * @param tenantId Tenant context
     * @param entityType Entity type to check
     * @return true if policy exists
     */
    @Query("""
        SELECT COUNT(drp) > 0 FROM DataRetentionPolicy drp
        WHERE drp.tenantId = :tenantId
        AND drp.entityType = :entityType
        """)
    boolean existsByEntityType(
        @Param("tenantId") UUID tenantId,
        @Param("entityType") EntityType entityType
    );

    /**
     * Get total archived records across all policies
     * Used for storage cost analysis
     *
     * @param tenantId Tenant context
     * @return Sum of records archived
     */
    @Query("""
        SELECT COALESCE(SUM(drp.recordsArchived), 0) FROM DataRetentionPolicy drp
        WHERE drp.tenantId = :tenantId
        """)
    long getTotalArchivedRecords(
        @Param("tenantId") UUID tenantId
    );
}
