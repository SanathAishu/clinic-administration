package com.clinic.backend.repository;

import com.clinic.common.entity.operational.Inventory;
import com.clinic.common.enums.InventoryCategory;
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

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    // Tenant-scoped queries
    Page<Inventory> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<Inventory> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // Item name search
    @Query("SELECT i FROM Inventory i WHERE i.tenantId = :tenantId AND " +
           "LOWER(i.itemName) LIKE LOWER(CONCAT('%', :itemName, '%')) AND i.deletedAt IS NULL")
    List<Inventory> findByItemNameContaining(@Param("tenantId") UUID tenantId, @Param("itemName") String itemName);

    // Item code lookup (unique per tenant)
    Optional<Inventory> findByItemCodeAndTenantIdAndDeletedAtIsNull(String itemCode, UUID tenantId);

    boolean existsByItemCodeAndTenantIdAndDeletedAtIsNull(String itemCode, UUID tenantId);

    // Category queries
    List<Inventory> findByTenantIdAndCategoryAndDeletedAtIsNull(UUID tenantId, InventoryCategory category);

    Page<Inventory> findByTenantIdAndCategoryInAndDeletedAtIsNull(UUID tenantId, List<InventoryCategory> categories, Pageable pageable);

    // Stock level queries (Cardinality constraints)
    @Query("SELECT i FROM Inventory i WHERE i.tenantId = :tenantId AND i.currentStock <= i.minimumStock AND i.deletedAt IS NULL")
    List<Inventory> findLowStockItems(@Param("tenantId") UUID tenantId);

    @Query("SELECT i FROM Inventory i WHERE i.tenantId = :tenantId AND i.currentStock = 0 AND i.deletedAt IS NULL")
    List<Inventory> findOutOfStockItems(@Param("tenantId") UUID tenantId);

    @Query("SELECT i FROM Inventory i WHERE i.tenantId = :tenantId AND i.currentStock > i.minimumStock AND i.deletedAt IS NULL")
    List<Inventory> findAdequateStockItems(@Param("tenantId") UUID tenantId);

    // Expiry date queries
    @Query("SELECT i FROM Inventory i WHERE i.tenantId = :tenantId AND i.expiryDate IS NOT NULL AND " +
           "i.expiryDate <= :date AND i.deletedAt IS NULL")
    List<Inventory> findExpiredItems(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);

    @Query("SELECT i FROM Inventory i WHERE i.tenantId = :tenantId AND i.expiryDate IS NOT NULL AND " +
           "i.expiryDate BETWEEN :startDate AND :endDate AND i.deletedAt IS NULL")
    List<Inventory> findExpiringItems(@Param("tenantId") UUID tenantId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    // Batch number queries
    List<Inventory> findByBatchNumberAndTenantIdAndDeletedAtIsNull(String batchNumber, UUID tenantId);

    // Manufacturer queries
    @Query("SELECT i FROM Inventory i WHERE i.tenantId = :tenantId AND " +
           "LOWER(i.manufacturer) LIKE LOWER(CONCAT('%', :manufacturer, '%')) AND i.deletedAt IS NULL")
    List<Inventory> findByManufacturerContaining(@Param("tenantId") UUID tenantId, @Param("manufacturer") String manufacturer);

    // Counting
    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    long countByTenantIdAndCategoryAndDeletedAtIsNull(UUID tenantId, InventoryCategory category);

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.tenantId = :tenantId AND i.currentStock <= i.minimumStock AND i.deletedAt IS NULL")
    long countLowStockItems(@Param("tenantId") UUID tenantId);
}
