package com.clinic.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for billing operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingResponse {

    private UUID id;
    private UUID tenantId;
    private UUID patientId;
    private UUID appointmentId;

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
    private String paymentReference;

    private Map<String, Object> lineItems;

    private Instant createdAt;
    private Instant updatedAt;
}
