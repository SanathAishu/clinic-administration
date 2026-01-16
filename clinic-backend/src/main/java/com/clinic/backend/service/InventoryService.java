package com.clinic.backend.service;

import com.clinic.common.entity.operational.Inventory;
import com.clinic.backend.repository.InventoryRepository;
import com.clinic.common.enums.InventoryCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional
    public Inventory createInventoryItem(Inventory inventory, UUID tenantId) {
        log.debug("Creating inventory item: {}", inventory.getItemName());

        inventory.setTenantId(tenantId);

        if (inventory.getCurrentStock() == null) {
            inventory.setCurrentStock(0);
        }

        if (inventory.getMinimumStock() == null) {
            inventory.setMinimumStock(0);
        }

        // Validate item code uniqueness
        if (inventory.getItemCode() != null) {
            Optional<Inventory> existing = inventoryRepository.findByItemCodeAndTenantIdAndDeletedAtIsNull(
                inventory.getItemCode(), tenantId);
            if (existing.isPresent()) {
                throw new IllegalArgumentException("Item code already exists: " + inventory.getItemCode());
            }
        }

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Created inventory item: {} (Code: {})", saved.getId(), saved.getItemCode());
        return saved;
    }

    public Inventory getInventoryItemById(UUID id, UUID tenantId) {
        return inventoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + id));
    }

    public Inventory getInventoryItemByCode(String itemCode, UUID tenantId) {
        return inventoryRepository.findByItemCodeAndTenantIdAndDeletedAtIsNull(itemCode, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found with code: " + itemCode));
    }

    public Page<Inventory> getAllInventoryItems(UUID tenantId, Pageable pageable) {
        return inventoryRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
    }

    public List<Inventory> getInventoryByCategory(UUID tenantId, InventoryCategory category) {
        return inventoryRepository.findByTenantIdAndCategoryAndDeletedAtIsNull(tenantId, category);
    }

    public List<Inventory> getLowStockItems(UUID tenantId) {
        return inventoryRepository.findLowStockItems(tenantId);
    }

    public List<Inventory> getOutOfStockItems(UUID tenantId) {
        return inventoryRepository.findOutOfStockItems(tenantId);
    }

    public List<Inventory> searchInventory(UUID tenantId, String search) {
        return inventoryRepository.findByItemNameContaining(tenantId, search);
    }

    @Transactional
    public Inventory adjustStock(UUID id, UUID tenantId, Integer newStock) {
        Inventory inventory = getInventoryItemById(id, tenantId);

        if (newStock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }

        inventory.setCurrentStock(newStock);
        Inventory saved = inventoryRepository.save(inventory);
        log.info("Adjusted stock for {}: {} units", saved.getItemName(), newStock);
        return saved;
    }

    @Transactional
    public Inventory addStock(UUID id, UUID tenantId, Integer quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Inventory inventory = getInventoryItemById(id, tenantId);
        inventory.setCurrentStock(inventory.getCurrentStock() + quantity);
        Inventory saved = inventoryRepository.save(inventory);
        log.info("Added {} units to {}", quantity, saved.getItemName());
        return saved;
    }

    @Transactional
    public Inventory removeStock(UUID id, UUID tenantId, Integer quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Inventory inventory = getInventoryItemById(id, tenantId);

        if (inventory.getCurrentStock() < quantity) {
            throw new IllegalStateException(
                    String.format("Insufficient stock: available=%d, requested=%d",
                            inventory.getCurrentStock(), quantity));
        }

        inventory.setCurrentStock(inventory.getCurrentStock() - quantity);
        Inventory saved = inventoryRepository.save(inventory);
        log.info("Removed {} units from {}", quantity, saved.getItemName());
        return saved;
    }

    @Transactional
    public Inventory updateInventoryItem(UUID id, UUID tenantId, Inventory updates) {
        Inventory inventory = getInventoryItemById(id, tenantId);

        if (updates.getItemName() != null) inventory.setItemName(updates.getItemName());
        if (updates.getDescription() != null) inventory.setDescription(updates.getDescription());
        if (updates.getCategory() != null) inventory.setCategory(updates.getCategory());
        if (updates.getMinimumStock() != null) inventory.setMinimumStock(updates.getMinimumStock());
        if (updates.getUnitPrice() != null) inventory.setUnitPrice(updates.getUnitPrice());
        if (updates.getUnit() != null) inventory.setUnit(updates.getUnit());
        if (updates.getManufacturer() != null) inventory.setManufacturer(updates.getManufacturer());
        if (updates.getBatchNumber() != null) inventory.setBatchNumber(updates.getBatchNumber());
        if (updates.getExpiryDate() != null) inventory.setExpiryDate(updates.getExpiryDate());

        if (updates.getItemCode() != null && !updates.getItemCode().equals(inventory.getItemCode())) {
            Optional<Inventory> existing = inventoryRepository.findByItemCodeAndTenantIdAndDeletedAtIsNull(
                updates.getItemCode(), tenantId);
            if (existing.isPresent()) {
                throw new IllegalArgumentException("Item code already exists: " + updates.getItemCode());
            }
            inventory.setItemCode(updates.getItemCode());
        }

        return inventoryRepository.save(inventory);
    }

    @Transactional
    public void softDeleteInventoryItem(UUID id, UUID tenantId) {
        Inventory inventory = getInventoryItemById(id, tenantId);
        inventory.softDelete();
        inventoryRepository.save(inventory);
        log.info("Soft deleted inventory item: {}", id);
    }

    public long countInventoryItems(UUID tenantId) {
        return inventoryRepository.countByTenantIdAndDeletedAtIsNull(tenantId);
    }
}
