package com.clinic.backend.entity;

import com.clinic.common.entity.SoftDeletableEntity;
import com.clinic.common.enums.PaymentMethod;
import com.clinic.common.enums.PaymentStatus;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "billing", indexes = {
    @Index(name = "idx_billing_tenant", columnList = "tenant_id"),
    @Index(name = "idx_billing_patient", columnList = "patient_id, invoice_date"),
    @Index(name = "idx_billing_invoice", columnList = "invoice_number"),
    @Index(name = "idx_billing_payment_status", columnList = "payment_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Billing extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @NotNull
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    // Billing Details
    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Invoice number is required")
    @Size(max = 50)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false)
    @NotNull
    private LocalDate invoiceDate = LocalDate.now();

    // Amounts
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    @NotNull
    @DecimalMin(value = "0.0", message = "Subtotal cannot be negative")
    private BigDecimal subtotal;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Discount cannot be negative")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Tax cannot be negative")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    @NotNull
    @DecimalMin(value = "0.0", message = "Total cannot be negative")
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Paid amount cannot be negative")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "balance_amount", nullable = false, precision = 10, scale = 2)
    @NotNull
    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
    private BigDecimal balanceAmount;

    // Payment Details
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @NotNull
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_reference", length = 100)
    @Size(max = 100)
    private String paymentReference;

    // Line Items (JSONB for flexibility)
    @Type(JsonBinaryType.class)
    @Column(name = "line_items", nullable = false, columnDefinition = "jsonb")
    @NotNull
    private Map<String, Object> lineItems;

    // Metadata
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @NotNull
    private User createdBy;

    // Calculate total and balance
    public void calculateAmounts() {
        this.totalAmount = subtotal
            .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO)
            .add(taxAmount != null ? taxAmount : BigDecimal.ZERO);

        this.balanceAmount = totalAmount
            .subtract(paidAmount != null ? paidAmount : BigDecimal.ZERO);

        updatePaymentStatus();
    }

    private void updatePaymentStatus() {
        if (balanceAmount.compareTo(BigDecimal.ZERO) == 0 && paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.paymentStatus = PaymentStatus.PAID;
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0 && balanceAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.paymentStatus = PaymentStatus.PARTIALLY_PAID;
        }
    }

    public void recordPayment(BigDecimal amount, PaymentMethod method, String reference) {
        this.paidAmount = (this.paidAmount != null ? this.paidAmount : BigDecimal.ZERO).add(amount);
        this.paymentMethod = method;
        this.paymentReference = reference;
        this.paymentDate = LocalDate.now();
        calculateAmounts();
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        calculateAmounts();
    }
}
