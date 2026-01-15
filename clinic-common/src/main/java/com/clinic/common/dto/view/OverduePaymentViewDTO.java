package com.clinic.common.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for v_overdue_payments database view.
 * Used for overdue payment records with aging buckets.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverduePaymentViewDTO {

    private UUID id;
    private UUID tenantId;
    private UUID patientId;
    private String patientName;
    private String patientPhone;
    private String patientEmail;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private String paymentStatus;
    private Integer daysOverdue;
    private String agingBucket;  // 0-30 days, 31-60 days, 61-90 days, 90+ days
    private BigDecimal priorityScore;  // days_overdue * balance_amount
}
