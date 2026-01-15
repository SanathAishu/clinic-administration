package com.clinic.backend.entity;

import com.clinic.common.entity.TenantAwareEntity;
import com.clinic.common.enums.TransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_transactions", indexes = {
    @Index(name = "idx_inventory_trans_tenant", columnList = "tenant_id"),
    @Index(name = "idx_inventory_trans_inventory", columnList = "inventory_id, transaction_date"),
    @Index(name = "idx_inventory_trans_type", columnList = "transaction_type, transaction_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    @NotNull
    private Inventory inventory;

    // Transaction Details
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    @NotNull
    private TransactionType transactionType;

    @Column(name = "quantity", nullable = false)
    @NotNull
    private Integer quantity;

    @Column(name = "unit_price", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Unit price cannot be negative")
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Total amount cannot be negative")
    private BigDecimal totalAmount;

    // Reference
    @Column(name = "reference_type", length = 100)
    @Size(max = 100)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    // Additional Information
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "supplier_name")
    @Size(max = 255)
    private String supplierName;

    // Stock Tracking
    @Column(name = "stock_before", nullable = false)
    @NotNull
    @Min(value = 0, message = "Stock before cannot be negative")
    private Integer stockBefore;

    @Column(name = "stock_after", nullable = false)
    @NotNull
    @Min(value = 0, message = "Stock after cannot be negative")
    private Integer stockAfter;

    // Metadata
    @Column(name = "transaction_date", nullable = false)
    @NotNull
    private Instant transactionDate = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @NotNull
    private User createdBy;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (transactionDate == null) {
            transactionDate = Instant.now();
        }
        if (totalAmount == null && unitPrice != null) {
            totalAmount = unitPrice.multiply(BigDecimal.valueOf(Math.abs(quantity)));
        }
    }
}
