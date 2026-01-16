package com.clinic.backend.repository;

import com.clinic.common.entity.operational.InventoryTransaction;
import com.clinic.common.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    // Inventory transaction history (Immutable sequence - audit trail)
    @Query("SELECT it FROM InventoryTransaction it WHERE it.inventory.id = :inventoryId AND it.tenantId = :tenantId " +
           "ORDER BY it.transactionDate DESC")
    Page<InventoryTransaction> findInventoryTransactions(@Param("inventoryId") UUID inventoryId,
                                                          @Param("tenantId") UUID tenantId,
                                                          Pageable pageable);

    List<InventoryTransaction> findByInventoryIdAndTenantIdOrderByTransactionDateDesc(UUID inventoryId, UUID tenantId);

    // Transaction type queries
    List<InventoryTransaction> findByTenantIdAndTransactionType(UUID tenantId, TransactionType transactionType);

    @Query("SELECT it FROM InventoryTransaction it WHERE it.inventory.id = :inventoryId AND it.tenantId = :tenantId AND " +
           "it.transactionType = :type ORDER BY it.transactionDate DESC")
    List<InventoryTransaction> findByInventoryAndType(@Param("inventoryId") UUID inventoryId,
                                                       @Param("tenantId") UUID tenantId,
                                                       @Param("type") TransactionType type);

    // Date range queries (Temporal sequence)
    @Query("SELECT it FROM InventoryTransaction it WHERE it.tenantId = :tenantId AND " +
           "it.transactionDate BETWEEN :startDate AND :endDate ORDER BY it.transactionDate DESC")
    Page<InventoryTransaction> findByDateRange(@Param("tenantId") UUID tenantId,
                                                @Param("startDate") Instant startDate,
                                                @Param("endDate") Instant endDate,
                                                Pageable pageable);

    // Reference-based queries
    @Query("SELECT it FROM InventoryTransaction it WHERE it.referenceType = :referenceType AND " +
           "it.referenceId = :referenceId AND it.tenantId = :tenantId ORDER BY it.transactionDate DESC")
    List<InventoryTransaction> findByReference(@Param("referenceType") String referenceType,
                                                @Param("referenceId") UUID referenceId,
                                                @Param("tenantId") UUID tenantId);

    // Created by user
    @Query("SELECT it FROM InventoryTransaction it WHERE it.createdBy.id = :userId AND it.tenantId = :tenantId " +
           "ORDER BY it.transactionDate DESC")
    Page<InventoryTransaction> findByCreatedBy(@Param("userId") UUID userId,
                                                @Param("tenantId") UUID tenantId,
                                                Pageable pageable);

    // Supplier queries
    @Query("SELECT it FROM InventoryTransaction it WHERE it.tenantId = :tenantId AND " +
           "LOWER(it.supplierName) LIKE LOWER(CONCAT('%', :supplier, '%')) ORDER BY it.transactionDate DESC")
    List<InventoryTransaction> findBySupplierContaining(@Param("tenantId") UUID tenantId, @Param("supplier") String supplier);

    // Financial analytics
    @Query("SELECT SUM(it.totalAmount) FROM InventoryTransaction it WHERE it.tenantId = :tenantId AND " +
           "it.transactionType = :type AND it.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalByType(@Param("tenantId") UUID tenantId,
                                     @Param("type") TransactionType type,
                                     @Param("startDate") Instant startDate,
                                     @Param("endDate") Instant endDate);

    // Stock validation (Invariant: stock_after = stock_before + IN - OUT)
    @Query("SELECT it FROM InventoryTransaction it WHERE it.inventory.id = :inventoryId AND it.tenantId = :tenantId " +
           "ORDER BY it.transactionDate ASC")
    List<InventoryTransaction> findTransactionsForStockValidation(@Param("inventoryId") UUID inventoryId,
                                                                   @Param("tenantId") UUID tenantId);

    // Recent transactions
    @Query("SELECT it FROM InventoryTransaction it WHERE it.tenantId = :tenantId AND it.transactionDate >= :since " +
           "ORDER BY it.transactionDate DESC")
    List<InventoryTransaction> findRecentTransactions(@Param("tenantId") UUID tenantId, @Param("since") Instant since);

    // Counting
    long countByInventoryIdAndTenantId(UUID inventoryId, UUID tenantId);

    long countByTenantIdAndTransactionType(UUID tenantId, TransactionType transactionType);

    // Additional methods for service
    @Query("SELECT it FROM InventoryTransaction it WHERE it.id = :id AND it.tenantId = :tenantId")
    java.util.Optional<InventoryTransaction> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT it FROM InventoryTransaction it WHERE it.inventory.id = :inventoryId AND it.tenantId = :tenantId " +
           "ORDER BY it.transactionDate DESC")
    Page<InventoryTransaction> findByInventoryIdAndTenantId(@Param("inventoryId") UUID inventoryId,
                                                             @Param("tenantId") UUID tenantId,
                                                             Pageable pageable);

    @Query("SELECT it FROM InventoryTransaction it WHERE it.createdBy.id = :userId AND it.tenantId = :tenantId " +
           "ORDER BY it.transactionDate DESC")
    List<InventoryTransaction> findByPerformedByIdAndTenantId(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);
}
