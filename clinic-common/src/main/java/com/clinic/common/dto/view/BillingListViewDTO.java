package com.clinic.common.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for v_billing_list database view.
 * Used for billing listing with payment status and aging calculations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingListViewDTO {

    private UUID id;
    private UUID tenantId;
    private UUID patientId;
    private String patientName;
    private String patientPhone;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private String paymentStatus;
    private String paymentMethod;
    private LocalDate paymentDate;
    private UUID appointmentId;
    private Instant appointmentTime;
    private String doctorName;
    private String billingStatus;  // FULLY_PAID, PARTIAL, OVERDUE, PENDING
    private Integer daysOverdue;
    private Instant createdAt;
    private Instant updatedAt;
}
