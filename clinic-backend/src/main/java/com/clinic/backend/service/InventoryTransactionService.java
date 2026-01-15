package com.clinic.backend.service;

import com.clinic.common.entity.operational.InventoryTransaction;
import com.clinic.backend.repository.InventoryTransactionRepository;
import com.clinic.common.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryTransactionService {

    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryService inventoryService;

    @Transactional
    public InventoryTransaction createTransaction(InventoryTransaction transaction, UUID tenantId) {
        log.debug("Creating inventory transaction for item: {}", transaction.getInventory().getId());

        transaction.setTenantId(tenantId);

        if (transaction.getTransactionDate() == null) {
            transaction.setTransactionDate(Instant.now());
        }

        // Get current stock
        Integer stockBefore = transaction.getInventory().getCurrentStock();
        transaction.setStockBefore(stockBefore);

        // Apply transaction to inventory
        // PURCHASE and RETURN increase stock, SALE and EXPIRY decrease stock
        if (transaction.getTransactionType() == TransactionType.PURCHASE ||
            transaction.getTransactionType() == TransactionType.RETURN) {
            inventoryService.addStock(transaction.getInventory().getId(), tenantId, transaction.getQuantity());
        } else if (transaction.getTransactionType() == TransactionType.SALE ||
                   transaction.getTransactionType() == TransactionType.EXPIRY) {
            inventoryService.removeStock(transaction.getInventory().getId(), tenantId, transaction.getQuantity());
        }
        // ADJUSTMENT and TRANSFER are handled separately based on quantity sign

        // Get updated stock
        Integer stockAfter = transaction.getInventory().getCurrentStock();
        transaction.setStockAfter(stockAfter);

        // Invariant: stockAfter = stockBefore + (PURCHASE/RETURN) - (SALE/EXPIRY)
        Integer expectedStock;
        if (transaction.getTransactionType() == TransactionType.PURCHASE ||
            transaction.getTransactionType() == TransactionType.RETURN) {
            expectedStock = stockBefore + transaction.getQuantity();
        } else if (transaction.getTransactionType() == TransactionType.SALE ||
                   transaction.getTransactionType() == TransactionType.EXPIRY) {
            expectedStock = stockBefore - transaction.getQuantity();
        } else {
            expectedStock = stockAfter; // Skip validation for ADJUSTMENT/TRANSFER
        }

        if (!stockAfter.equals(expectedStock)) {
            throw new IllegalStateException(
                    String.format("Invariant violation: stockAfter=%d, expected=%d",
                            stockAfter, expectedStock));
        }

        InventoryTransaction saved = inventoryTransactionRepository.save(transaction);
        log.info("Created inventory transaction: {} (Type: {}, Qty: {})",
                saved.getId(), saved.getTransactionType(), saved.getQuantity());
        return saved;
    }

    public InventoryTransaction getTransactionById(UUID id, UUID tenantId) {
        return inventoryTransactionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
    }

    public Page<InventoryTransaction> getTransactionsForInventoryItem(UUID inventoryId, UUID tenantId, Pageable pageable) {
        return inventoryTransactionRepository.findByInventoryIdAndTenantId(inventoryId, tenantId, pageable);
    }

    public List<InventoryTransaction> getTransactionsByType(UUID tenantId, TransactionType transactionType) {
        return inventoryTransactionRepository.findByTenantIdAndTransactionType(tenantId, transactionType);
    }

    public List<InventoryTransaction> getRecentTransactions(UUID tenantId, Instant since) {
        return inventoryTransactionRepository.findRecentTransactions(tenantId, since);
    }

    public List<InventoryTransaction> getTransactionsByUser(UUID userId, UUID tenantId) {
        return inventoryTransactionRepository.findByPerformedByIdAndTenantId(userId, tenantId);
    }

    @Transactional
    public InventoryTransaction updateTransaction(UUID id, UUID tenantId, InventoryTransaction updates) {
        InventoryTransaction transaction = getTransactionById(id, tenantId);

        // Only allow updating notes, reference type/id, and supplier
        if (updates.getNotes() != null) transaction.setNotes(updates.getNotes());
        if (updates.getReferenceType() != null) transaction.setReferenceType(updates.getReferenceType());
        if (updates.getReferenceId() != null) transaction.setReferenceId(updates.getReferenceId());
        if (updates.getSupplierName() != null) transaction.setSupplierName(updates.getSupplierName());

        return inventoryTransactionRepository.save(transaction);
    }

    @Transactional
    public void deleteTransaction(UUID id, UUID tenantId) {
        InventoryTransaction transaction = getTransactionById(id, tenantId);
        inventoryTransactionRepository.delete(transaction);
        log.info("Deleted inventory transaction: {}", id);
    }

    public long countTransactionsForItem(UUID inventoryId, UUID tenantId) {
        return inventoryTransactionRepository.countByInventoryIdAndTenantId(inventoryId, tenantId);
    }
}
