package com.clinic.backend.repository;

import com.clinic.common.entity.operational.InventoryAnalytics;
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
 * Repository for InventoryAnalytics entity.
 *
 * Provides data access methods for demand statistics and analytics.
 * Used by InventoryOptimizationService to:
 * - Retrieve historical demand patterns
 * - Calculate demand statistics for ROP
 * - Track demand trends over time
 */
@Repository
public interface InventoryAnalyticsRepository extends JpaRepository<InventoryAnalytics, UUID> {

    /**
     * Find analytics for a specific inventory item ordered by period (most recent first).
     */
    @Query("SELECT ia FROM InventoryAnalytics ia " +
           "WHERE ia.inventory.id = :inventoryId AND ia.tenantId = :tenantId AND ia.deletedAt IS NULL " +
           "ORDER BY ia.periodStart DESC")
    List<InventoryAnalytics> findByInventoryIdAndTenantId(
        @Param("inventoryId") UUID inventoryId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Find the most recent analytics for an inventory item.
     */
    @Query("SELECT ia FROM InventoryAnalytics ia " +
           "WHERE ia.inventory.id = :inventoryId AND ia.tenantId = :tenantId AND ia.deletedAt IS NULL " +
           "ORDER BY ia.periodStart DESC " +
           "LIMIT 1")
    Optional<InventoryAnalytics> findMostRecentByInventoryId(
        @Param("inventoryId") UUID inventoryId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Find analytics within a specific date range.
     */
    @Query("SELECT ia FROM InventoryAnalytics ia " +
           "WHERE ia.inventory.id = :inventoryId AND ia.tenantId = :tenantId " +
           "AND ia.deletedAt IS NULL " +
           "AND ia.periodStart >= :startDate AND ia.periodEnd <= :endDate " +
           "ORDER BY ia.periodStart DESC")
    List<InventoryAnalytics> findByInventoryIdInDateRange(
        @Param("inventoryId") UUID inventoryId,
        @Param("tenantId") UUID tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all analytics for a tenant paginated.
     */
    @Query("SELECT ia FROM InventoryAnalytics ia " +
           "WHERE ia.tenantId = :tenantId AND ia.deletedAt IS NULL " +
           "ORDER BY ia.periodStart DESC")
    Page<InventoryAnalytics> findByTenantId(
        @Param("tenantId") UUID tenantId,
        Pageable pageable
    );

    /**
     * Find analytics for all inventory items in a tenant.
     */
    @Query("SELECT ia FROM InventoryAnalytics ia " +
           "WHERE ia.tenantId = :tenantId AND ia.deletedAt IS NULL " +
           "AND ia.periodStart >= :fromDate")
    List<InventoryAnalytics> findByTenantIdAndRecentPeriods(
        @Param("tenantId") UUID tenantId,
        @Param("fromDate") LocalDate fromDate
    );

    /**
     * Count analytics records for an inventory item.
     */
    @Query("SELECT COUNT(ia) FROM InventoryAnalytics ia " +
           "WHERE ia.inventory.id = :inventoryId AND ia.tenantId = :tenantId AND ia.deletedAt IS NULL")
    long countByInventoryIdAndTenantId(
        @Param("inventoryId") UUID inventoryId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Check if analytics already exist for a specific period.
     */
    @Query("SELECT COUNT(ia) > 0 FROM InventoryAnalytics ia " +
           "WHERE ia.inventory.id = :inventoryId AND ia.tenantId = :tenantId " +
           "AND ia.deletedAt IS NULL " +
           "AND ia.periodStart = :periodStart AND ia.periodEnd = :periodEnd")
    boolean existsByInventoryIdAndPeriod(
        @Param("inventoryId") UUID inventoryId,
        @Param("tenantId") UUID tenantId,
        @Param("periodStart") LocalDate periodStart,
        @Param("periodEnd") LocalDate periodEnd
    );

    /**
     * Delete analytics by inventory item (soft delete).
     * Used when inventory item is deleted.
     */
    @Query("DELETE FROM InventoryAnalytics ia " +
           "WHERE ia.inventory.id = :inventoryId AND ia.tenantId = :tenantId")
    void deleteByInventoryId(
        @Param("inventoryId") UUID inventoryId,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Find analytics for items with high demand variability (σ > threshold).
     * Used to identify items needing higher safety stock.
     */
    @Query("SELECT ia FROM InventoryAnalytics ia " +
           "WHERE ia.tenantId = :tenantId AND ia.deletedAt IS NULL " +
           "AND ia.demandStdDev > :stdDevThreshold " +
           "AND ia.periodStart >= :sinceDate " +
           "ORDER BY ia.demandStdDev DESC")
    List<InventoryAnalytics> findHighVariabilityItems(
        @Param("tenantId") UUID tenantId,
        @Param("stdDevThreshold") Double stdDevThreshold,
        @Param("sinceDate") LocalDate sinceDate
    );

    /**
     * Find analytics for stable items (σ < threshold).
     * Used to identify items with predictable demand.
     */
    @Query("SELECT ia FROM InventoryAnalytics ia " +
           "WHERE ia.tenantId = :tenantId AND ia.deletedAt IS NULL " +
           "AND ia.demandStdDev <= :stdDevThreshold " +
           "AND ia.periodStart >= :sinceDate " +
           "ORDER BY ia.avgDailyDemand DESC")
    List<InventoryAnalytics> findStableItems(
        @Param("tenantId") UUID tenantId,
        @Param("stdDevThreshold") Double stdDevThreshold,
        @Param("sinceDate") LocalDate sinceDate
    );
}
