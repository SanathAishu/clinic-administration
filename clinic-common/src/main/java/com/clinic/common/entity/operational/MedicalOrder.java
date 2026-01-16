package com.clinic.common.entity.operational;

import com.clinic.common.entity.SoftDeletableEntity;
import com.clinic.common.entity.core.Branch;
import com.clinic.common.entity.patient.Patient;
import com.clinic.common.entity.core.User;
import com.clinic.common.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Medical Order Entity
 * Tracks orders for braces, orthotic devices, surgical items, and other medical products
 * sent to manufacturers for custom fabrication
 */
@Entity
@Table(name = "medical_orders", indexes = {
    @Index(name = "idx_medical_orders_tenant", columnList = "tenant_id"),
    @Index(name = "idx_medical_orders_patient", columnList = "patient_id"),
    @Index(name = "idx_medical_orders_status", columnList = "status"),
    @Index(name = "idx_medical_orders_order_date", columnList = "order_date"),
    @Index(name = "idx_medical_orders_branch", columnList = "branch_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalOrder extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @NotNull(message = "Patient is required")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    // Order Details
    @Column(name = "product_name", nullable = false, length = 255)
    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name cannot exceed 255 characters")
    private String productName;

    @Column(name = "product_type", length = 100)
    @Size(max = 100, message = "Product type cannot exceed 100 characters")
    private String productType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantity", nullable = false)
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    // Manufacturer Details
    @Column(name = "manufacturer_name", length = 255)
    @Size(max = 255, message = "Manufacturer name cannot exceed 255 characters")
    private String manufacturerName;

    @Column(name = "manufacturer_email", length = 255)
    @Email(message = "Manufacturer email must be valid")
    @Size(max = 255, message = "Manufacturer email cannot exceed 255 characters")
    private String manufacturerEmail;

    @Column(name = "manufacturer_phone", length = 15)
    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Manufacturer phone must be valid")
    private String manufacturerPhone;

    // Pricing
    @Column(name = "unit_price", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Unit price cannot be negative")
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Total amount cannot be negative")
    private BigDecimal totalAmount;

    // Order Status (State Machine)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull(message = "Order status is required")
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    // Dates (Sequences & Recurrence)
    @Column(name = "order_date", nullable = false)
    @NotNull(message = "Order date is required")
    @Builder.Default
    private LocalDate orderDate = LocalDate.now();

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    // State Tracking Timestamps
    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    // Additional Information
    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    @Column(name = "tracking_number", length = 100)
    @Size(max = 100, message = "Tracking number cannot exceed 100 characters")
    private String trackingNumber;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Notification tracking
    @Column(name = "patient_notified_at")
    private Instant patientNotifiedAt;

    // Metadata
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @NotNull(message = "Created by is required")
    private User createdBy;

    /**
     * State machine methods (Discrete Math: State Transitions)
     */
    public void markAsSent() {
        validateStatusTransition(status, OrderStatus.SENT);
        this.status = OrderStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markAsInProduction() {
        validateStatusTransition(status, OrderStatus.IN_PRODUCTION);
        this.status = OrderStatus.IN_PRODUCTION;
    }

    public void markAsShipped(String trackingNumber) {
        validateStatusTransition(status, OrderStatus.SHIPPED);
        this.status = OrderStatus.SHIPPED;
        this.trackingNumber = trackingNumber;
    }

    public void markAsReceived() {
        validateStatusTransition(status, OrderStatus.RECEIVED);
        this.status = OrderStatus.RECEIVED;
        this.receivedAt = Instant.now();
        this.actualDeliveryDate = LocalDate.now();
    }

    public void markAsReadyForPickup() {
        validateStatusTransition(status, OrderStatus.READY_FOR_PICKUP);
        this.status = OrderStatus.READY_FOR_PICKUP;
        // Ready for patient notification
    }

    public void markAsDelivered() {
        validateStatusTransition(status, OrderStatus.DELIVERED);
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = Instant.now();
    }

    public void cancel() {
        if (status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel delivered order");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    /**
     * Validate state transitions (Discrete Math: DAG enforcement)
     */
    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        boolean isValidTransition = switch (from) {
            case PENDING -> to == OrderStatus.SENT || to == OrderStatus.CANCELLED;
            case SENT -> to == OrderStatus.IN_PRODUCTION || to == OrderStatus.CANCELLED;
            case IN_PRODUCTION -> to == OrderStatus.SHIPPED || to == OrderStatus.CANCELLED;
            case SHIPPED -> to == OrderStatus.RECEIVED || to == OrderStatus.CANCELLED;
            case RECEIVED -> to == OrderStatus.READY_FOR_PICKUP;
            case READY_FOR_PICKUP -> to == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!isValidTransition) {
            throw new IllegalStateException(
                String.format("Invalid status transition: %s to %s", from, to)
            );
        }
    }

    /**
     * Calculate total amount and validate invariants
     * Combined method: JPA allows only one @PrePersist/@PreUpdate per class
     * (Operations Research: Cost Calculation + Discrete Math: Invariants)
     */
    @PrePersist
    @PreUpdate
    protected void calculateAndValidate() {
        // Calculate total amount
        if (unitPrice != null && quantity != null) {
            this.totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        }

        // Temporal ordering: orderDate <= expectedDeliveryDate <= actualDeliveryDate
        if (expectedDeliveryDate != null && expectedDeliveryDate.isBefore(orderDate)) {
            throw new IllegalStateException(
                "Invariant violation: Expected delivery date cannot be before order date"
            );
        }

        if (actualDeliveryDate != null && actualDeliveryDate.isBefore(orderDate)) {
            throw new IllegalStateException(
                "Invariant violation: Actual delivery date cannot be before order date"
            );
        }

        // State-Timestamp consistency
        if (status == OrderStatus.SENT && sentAt == null) {
            throw new IllegalStateException(
                "Invariant violation: SENT status requires sentAt timestamp"
            );
        }

        if (status == OrderStatus.RECEIVED && receivedAt == null) {
            throw new IllegalStateException(
                "Invariant violation: RECEIVED status requires receivedAt timestamp"
            );
        }

        if (status == OrderStatus.DELIVERED && deliveredAt == null) {
            throw new IllegalStateException(
                "Invariant violation: DELIVERED status requires deliveredAt timestamp"
            );
        }

        if (status == OrderStatus.CANCELLED && cancelledAt == null) {
            throw new IllegalStateException(
                "Invariant violation: CANCELLED status requires cancelledAt timestamp"
            );
        }
    }
}
