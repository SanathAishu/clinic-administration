package com.clinic.backend.repository;

import com.clinic.common.entity.compliance.ComplianceMetrics;
import com.clinic.common.enums.ComplianceMetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ComplianceMetrics entity
 * Supports ISO 27001 A.18 (Compliance) monitoring and SLA reporting
 *
 * Key Features:
 * - Multi-tenant isolation
 * - Metric type filtering
 * - Date range queries for compliance windows
 * - Out-of-control metric detection
 * - Historical trend analysis
 */
@Repository
public interface ComplianceMetricsRepository extends JpaRepository<ComplianceMetrics, UUID> {

    /**
     * Find metrics for specific date
     * Used for daily compliance reports
     *
     * @param tenantId Tenant context
     * @param date Metric date
     * @return List of metrics for all types on given date
     */
    @Query("""
        SELECT cm FROM ComplianceMetrics cm
        WHERE cm.tenantId = :tenantId
        AND cm.metricDate = :date
        AND cm.deletedAt IS NULL
        """)
    List<ComplianceMetrics> findByMetricDate(
        @Param("tenantId") UUID tenantId,
        @Param("date") LocalDate date
    );

    /**
     * Find specific metric type for given date
     *
     * @param tenantId Tenant context
     * @param date Metric date
     * @param metricType Specific metric type to query
     * @return Optional containing metric if found
     */
    @Query("""
        SELECT cm FROM ComplianceMetrics cm
        WHERE cm.tenantId = :tenantId
        AND cm.metricDate = :date
        AND cm.metricType = :metricType
        AND cm.deletedAt IS NULL
        """)
    Optional<ComplianceMetrics> findByDateAndType(
        @Param("tenantId") UUID tenantId,
        @Param("date") LocalDate date,
        @Param("metricType") ComplianceMetricType metricType
    );

    /**
     * Find all out-of-control metrics (SLA violations)
     * Critical for alerting and escalation
     *
     * @param tenantId Tenant context
     * @param startDate Date range start
     * @return List of out-of-control metrics ordered by date descending
     */
    @Query("""
        SELECT cm FROM ComplianceMetrics cm
        WHERE cm.tenantId = :tenantId
        AND cm.metricDate >= :startDate
        AND cm.outOfControl = true
        AND cm.deletedAt IS NULL
        ORDER BY cm.metricDate DESC
        """)
    List<ComplianceMetrics> findOutOfControlMetrics(
        @Param("tenantId") UUID tenantId,
        @Param("startDate") LocalDate startDate
    );

    /**
     * Find recent metrics for specific type (30-day window)
     * Used for control limit calculation and trend analysis
     *
     * @param tenantId Tenant context
     * @param metricType Metric type to retrieve
     * @param days Number of days to look back
     * @return List of metrics for calculation
     */
    @Query("""
        SELECT cm FROM ComplianceMetrics cm
        WHERE cm.tenantId = :tenantId
        AND cm.metricType = :metricType
        AND cm.metricDate >= CURRENT_DATE - INTERVAL '1 day' * :days
        AND cm.deletedAt IS NULL
        ORDER BY cm.metricDate DESC
        """)
    List<ComplianceMetrics> findRecentByType(
        @Param("tenantId") UUID tenantId,
        @Param("metricType") ComplianceMetricType metricType,
        @Param("days") int days
    );

    /**
     * Find metrics within date range
     * Used for dashboard queries and compliance reports
     *
     * @param tenantId Tenant context
     * @param startDate Date range start
     * @param endDate Date range end
     * @return List of metrics within range ordered by date
     */
    @Query("""
        SELECT cm FROM ComplianceMetrics cm
        WHERE cm.tenantId = :tenantId
        AND cm.metricDate >= :startDate
        AND cm.metricDate <= :endDate
        AND cm.deletedAt IS NULL
        ORDER BY cm.metricDate DESC, cm.metricType
        """)
    List<ComplianceMetrics> findByDateRange(
        @Param("tenantId") UUID tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Calculate average compliance rate for metric type
     * Used for SLA tracking and goal analysis
     *
     * @param tenantId Tenant context
     * @param metricType Metric type to analyze
     * @param startDate Date range start
     * @param endDate Date range end
     * @return Average compliance rate (0-100) or 0 if no metrics
     */
    @Query("""
        SELECT COALESCE(AVG(cm.complianceRate), 0.0) FROM ComplianceMetrics cm
        WHERE cm.tenantId = :tenantId
        AND cm.metricType = :metricType
        AND cm.metricDate >= :startDate
        AND cm.metricDate <= :endDate
        AND cm.deletedAt IS NULL
        """)
    Double getAverageComplianceRate(
        @Param("tenantId") UUID tenantId,
        @Param("metricType") ComplianceMetricType metricType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find minimum compliance rate achieved
     * Used for worst-case analysis
     *
     * @param tenantId Tenant context
     * @param metricType Metric type to analyze
     * @param startDate Date range start
     * @param endDate Date range end
     * @return Minimum compliance rate achieved
     */
    @Query("""
        SELECT COALESCE(MIN(cm.complianceRate), 0.0) FROM ComplianceMetrics cm
        WHERE cm.tenantId = :tenantId
        AND cm.metricType = :metricType
        AND cm.metricDate >= :startDate
        AND cm.metricDate <= :endDate
        AND cm.deletedAt IS NULL
        """)
    Double getMinimumComplianceRate(
        @Param("tenantId") UUID tenantId,
        @Param("metricType") ComplianceMetricType metricType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Count violations for metric type within date range
     * Used for trending and alert generation
     *
     * @param tenantId Tenant context
     * @param metricType Metric type to count
     * @param startDate Date range start
     * @param endDate Date range end
     * @return Number of violations (out-of-control days)
     */
    @Query("""
        SELECT COUNT(cm) FROM ComplianceMetrics cm
        WHERE cm.tenantId = :tenantId
        AND cm.metricType = :metricType
        AND cm.outOfControl = true
        AND cm.metricDate >= :startDate
        AND cm.metricDate <= :endDate
        AND cm.deletedAt IS NULL
        """)
    long countViolations(
        @Param("tenantId") UUID tenantId,
        @Param("metricType") ComplianceMetricType metricType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
