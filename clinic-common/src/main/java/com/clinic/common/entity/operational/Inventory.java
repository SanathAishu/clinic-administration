package com.clinic.common.entity.operational;

import com.clinic.common.entity.core.User;
import com.clinic.common.entity.SoftDeletableEntity;
import com.clinic.common.entity.operational.Inventory;
import com.clinic.common.enums.InventoryCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "inventory", indexes = {
    @Index(name = "idx_inventory_tenant", columnList = "tenant_id"),
    @Index(name = "idx_inventory_category", columnList = "category"),
    @Index(name = "idx_inventory_low_stock", columnList = "tenant_id"),
    @Index(name = "idx_inventory_expiry", columnList = "expiry_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory extends SoftDeletableEntity {

    // Item Details
    @Column(name = "item_name", nullable = false)
    @NotBlank(message = "Item name is required")
    @Size(max = 255)
    private String itemName;

    @Column(name = "item_code", unique = true, length = 100)
    @Size(max = 100)
    private String itemCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    @NotNull
    private InventoryCategory category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Stock Information
    @Column(name = "current_stock", nullable = false)
    @NotNull
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer currentStock = 0;

    @Column(name = "minimum_stock", nullable = false)
    @NotNull
    @Min(value = 0, message = "Minimum stock cannot be negative")
    private Integer minimumStock = 0;

    @Column(name = "unit", nullable = false, length = 50)
    @NotBlank(message = "Unit is required")
    @Size(max = 50)
    private String unit;

    @Column(name = "unit_price", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Unit price cannot be negative")
    private BigDecimal unitPrice;

    // Medicine Specific
    @Column(name = "manufacturer")
    @Size(max = 255)
    private String manufacturer;

    @Column(name = "batch_number", length = 100)
    @Size(max = 100)
    private String batchNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // Storage
    @Column(name = "location", length = 100)
    @Size(max = 100)
    private String location;

    // Metadata
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @NotNull
    private User createdBy;

    // Helper methods
    public boolean isLowStock() {
        return currentStock <= minimumStock;
    }

    public boolean isExpiringSoon(int daysThreshold) {
        if (expiryDate == null) {
            return false;
        }
        return expiryDate.isBefore(LocalDate.now().plusDays(daysThreshold));
    }

    public boolean isExpired() {
        if (expiryDate == null) {
            return false;
        }
        return expiryDate.isBefore(LocalDate.now());
    }

    public void addStock(int quantity) {
        this.currentStock += quantity;
    }

    public void reduceStock(int quantity) {
        if (this.currentStock >= quantity) {
            this.currentStock -= quantity;
        } else {
            throw new IllegalArgumentException("Insufficient stock available");
        }
    }
}
