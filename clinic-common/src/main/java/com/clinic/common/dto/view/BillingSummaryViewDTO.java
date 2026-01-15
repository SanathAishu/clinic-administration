package com.clinic.common.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for mv_billing_summary_by_period materialized view.
 * Used for financial reporting with revenue aggregations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingSummaryViewDTO {

    private UUID tenantId;

    // Time Period Grouping
    private LocalDate periodDay;
    private LocalDate periodWeek;
    private LocalDate periodMonth;
    private LocalDate periodYear;

    // Invoice Counts by Status
    private Long totalInvoices;
    private Long paidInvoices;
    private Long pendingInvoices;
    private Long partialInvoices;
    private Long cancelledInvoices;

    // Revenue Aggregations
    private BigDecimal totalRevenue;
    private BigDecimal collectedRevenue;
    private BigDecimal outstandingBalance;

    // Payment Method Distribution (Counts)
    private Long cashPayments;
    private Long cardPayments;
    private Long upiPayments;
    private Long insurancePayments;

    // Payment Method Distribution (Revenue)
    private BigDecimal cashRevenue;
    private BigDecimal cardRevenue;
    private BigDecimal upiRevenue;
    private BigDecimal insuranceRevenue;
}
