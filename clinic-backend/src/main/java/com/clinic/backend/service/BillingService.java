package com.clinic.backend.service;

import com.clinic.backend.repository.BillingViewRepository;
import com.clinic.common.dto.view.BillingListViewDTO;
import com.clinic.common.dto.view.BillingSummaryViewDTO;
import com.clinic.common.dto.view.OverduePaymentViewDTO;
import com.clinic.common.entity.operational.Billing;
import com.clinic.backend.repository.BillingRepository;
import com.clinic.common.enums.PaymentMethod;
import com.clinic.common.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingService {

    private final BillingRepository billingRepository;
    private final BillingViewRepository billingViewRepository;

    @Transactional
    public Billing createBilling(Billing billing, UUID tenantId) {
        log.debug("Creating billing for patient: {}", billing.getPatient().getId());

        billing.setTenantId(tenantId);

        if (billing.getInvoiceDate() == null) {
            billing.setInvoiceDate(LocalDate.now());
        }

        if (billing.getPaymentStatus() == null) {
            billing.setPaymentStatus(PaymentStatus.PENDING);
        }

        if (billing.getPaidAmount() == null) {
            billing.setPaidAmount(BigDecimal.ZERO);
        }

        // Calculate balance (Invariant: balance = total - paid)
        BigDecimal totalAmount = billing.getTotalAmount();
        BigDecimal paidAmount = billing.getPaidAmount();
        billing.setBalanceAmount(totalAmount.subtract(paidAmount));

        Billing saved = billingRepository.save(billing);
        log.info("Created billing: {} with invoice: {}", saved.getId(), saved.getInvoiceNumber());
        return saved;
    }

    public Billing getBillingById(UUID id, UUID tenantId) {
        return billingRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Billing not found: " + id));
    }

    public Billing getBillingByInvoiceNumber(String invoiceNumber, UUID tenantId) {
        return billingRepository.findByInvoiceNumberAndTenantId(invoiceNumber, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceNumber));
    }

    public Page<Billing> getBillingsForPatient(UUID patientId, UUID tenantId, Pageable pageable) {
        return billingRepository.findByPatientIdAndTenantIdAndDeletedAtIsNull(patientId, tenantId, pageable);
    }

    public List<Billing> getBillingsByStatus(UUID tenantId, PaymentStatus status) {
        return billingRepository.findByTenantIdAndPaymentStatus(tenantId, status);
    }

    public List<Billing> getPendingBillingsForPatient(UUID patientId, UUID tenantId) {
        return billingRepository.findPendingBillingsForPatient(patientId, tenantId);
    }

    public List<Billing> getOverdueBillings(UUID tenantId, LocalDate dueDate) {
        return billingRepository.findOverdueBillings(tenantId, dueDate);
    }

    @Transactional
    public Billing recordPayment(UUID id, UUID tenantId, BigDecimal paymentAmount, String paymentMethod) {
        Billing billing = getBillingById(id, tenantId);

        BigDecimal newPaidAmount = billing.getPaidAmount().add(paymentAmount);
        billing.setPaidAmount(newPaidAmount);

        // Recalculate balance (Invariant enforcement)
        BigDecimal newBalance = billing.getTotalAmount().subtract(newPaidAmount);
        billing.setBalanceAmount(newBalance);

        // Update payment status
        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            billing.setPaymentStatus(PaymentStatus.PAID);
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            billing.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        }

        // Set payment method if first payment
        if (billing.getPaymentMethod() == null && paymentMethod != null) {
            billing.setPaymentMethod(PaymentMethod.valueOf(paymentMethod));
        }

        Billing saved = billingRepository.save(billing);
        log.info("Recorded payment of {} for billing: {}", paymentAmount, saved.getId());
        return saved;
    }

    @Transactional
    public Billing markAsPaid(UUID id, UUID tenantId) {
        Billing billing = getBillingById(id, tenantId);
        billing.setPaidAmount(billing.getTotalAmount());
        billing.setBalanceAmount(BigDecimal.ZERO);
        billing.setPaymentStatus(PaymentStatus.PAID);
        Billing saved = billingRepository.save(billing);
        log.info("Marked billing as paid: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Billing cancelBilling(UUID id, UUID tenantId) {
        Billing billing = getBillingById(id, tenantId);
        billing.setPaymentStatus(PaymentStatus.CANCELLED);
        Billing saved = billingRepository.save(billing);
        log.info("Cancelled billing: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Billing updateBilling(UUID id, UUID tenantId, Billing updates) {
        Billing billing = getBillingById(id, tenantId);

        // Only allow updates if not paid or cancelled
        if (billing.getPaymentStatus() == PaymentStatus.PAID ||
            billing.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update paid or cancelled billing");
        }

        if (updates.getTotalAmount() != null) {
            billing.setTotalAmount(updates.getTotalAmount());
            // Recalculate balance
            billing.setBalanceAmount(updates.getTotalAmount().subtract(billing.getPaidAmount()));
        }

        if (updates.getDiscountAmount() != null) billing.setDiscountAmount(updates.getDiscountAmount());
        if (updates.getTaxAmount() != null) billing.setTaxAmount(updates.getTaxAmount());
        if (updates.getPaymentReference() != null) billing.setPaymentReference(updates.getPaymentReference());

        return billingRepository.save(billing);
    }

    @Transactional
    public void softDeleteBilling(UUID id, UUID tenantId) {
        Billing billing = getBillingById(id, tenantId);
        billing.softDelete();
        billingRepository.save(billing);
        log.info("Soft deleted billing: {}", id);
    }

    public BigDecimal getOutstandingBalanceForTenant(UUID tenantId) {
        return billingRepository.calculateOutstandingBalance(tenantId);
    }

    public BigDecimal getPatientOutstandingBalance(UUID patientId, UUID tenantId) {
        return billingRepository.calculatePatientOutstandingBalance(patientId, tenantId);
    }

    // ========================================================================
    // CQRS READ OPERATIONS (using database views)
    // ========================================================================

    /**
     * Get all billings using v_billing_list view (CQRS Read).
     */
    public List<BillingListViewDTO> getBillingListView(UUID tenantId) {
        return billingViewRepository.findAllByTenantId(tenantId);
    }

    /**
     * Get billing by ID using v_billing_list view (CQRS Read).
     */
    public Optional<BillingListViewDTO> getBillingDetailView(UUID id, UUID tenantId) {
        return billingViewRepository.findById(id, tenantId);
    }

    /**
     * Get billings for patient using view (CQRS Read).
     */
    public List<BillingListViewDTO> getPatientBillingsView(UUID patientId, UUID tenantId) {
        return billingViewRepository.findByPatientId(patientId, tenantId);
    }

    /**
     * Get billings by payment status using view (CQRS Read).
     */
    public List<BillingListViewDTO> getBillingsByPaymentStatusView(UUID tenantId, String status) {
        return billingViewRepository.findByPaymentStatus(tenantId, status);
    }

    /**
     * Get billings by billing status using view (CQRS Read).
     */
    public List<BillingListViewDTO> getBillingsByBillingStatusView(UUID tenantId, String billingStatus) {
        return billingViewRepository.findByBillingStatus(tenantId, billingStatus);
    }

    /**
     * Get billings within date range using view (CQRS Read).
     */
    public List<BillingListViewDTO> getBillingsByDateRangeView(UUID tenantId, LocalDate startDate, LocalDate endDate) {
        return billingViewRepository.findByDateRange(tenantId, startDate, endDate);
    }

    /**
     * Search billings using view (CQRS Read).
     */
    public List<BillingListViewDTO> searchBillingsView(UUID tenantId, String searchTerm) {
        return billingViewRepository.search(tenantId, searchTerm);
    }

    // ========================================================================
    // OVERDUE PAYMENTS (using v_overdue_payments view)
    // ========================================================================

    /**
     * Get all overdue payments using view (CQRS Read).
     */
    public List<OverduePaymentViewDTO> getOverduePaymentsView(UUID tenantId) {
        return billingViewRepository.findOverduePayments(tenantId);
    }

    /**
     * Get overdue payments by aging bucket using view (CQRS Read).
     */
    public List<OverduePaymentViewDTO> getOverdueByAgingBucketView(UUID tenantId, String agingBucket) {
        return billingViewRepository.findOverdueByAgingBucket(tenantId, agingBucket);
    }

    /**
     * Get total overdue amount for tenant.
     */
    public BigDecimal getTotalOverdueAmountView(UUID tenantId) {
        return billingViewRepository.getTotalOverdueAmount(tenantId);
    }

    /**
     * Get overdue count by aging bucket for dashboard.
     */
    public List<Object[]> getOverdueCountByAgingBucket(UUID tenantId) {
        return billingViewRepository.getOverdueCountByAgingBucket(tenantId);
    }

    // ========================================================================
    // BILLING SUMMARY (using mv_billing_summary_by_period materialized view)
    // ========================================================================

    /**
     * Get monthly billing summary using materialized view (CQRS Read).
     */
    public List<BillingSummaryViewDTO> getMonthlySummaryView(UUID tenantId, LocalDate month) {
        return billingViewRepository.findMonthlySummary(tenantId, month);
    }

    /**
     * Get billing summary for month range using materialized view (CQRS Read).
     */
    public List<BillingSummaryViewDTO> getSummaryByMonthRangeView(UUID tenantId, LocalDate startMonth, LocalDate endMonth) {
        return billingViewRepository.findSummaryByMonthRange(tenantId, startMonth, endMonth);
    }

    /**
     * Get yearly billing summary using materialized view (CQRS Read).
     */
    public List<BillingSummaryViewDTO> getYearlySummaryView(UUID tenantId, int year) {
        return billingViewRepository.findYearlySummary(tenantId, year);
    }

    /**
     * Get daily revenue for a month (for charts) using materialized view (CQRS Read).
     */
    public List<BillingSummaryViewDTO> getDailyRevenueForMonthView(UUID tenantId, LocalDate month) {
        return billingViewRepository.findDailyRevenueForMonth(tenantId, month);
    }

    /**
     * Get current month summary using materialized view (CQRS Read).
     */
    public Optional<BillingSummaryViewDTO> getCurrentMonthSummaryView(UUID tenantId) {
        return billingViewRepository.findCurrentMonthSummary(tenantId);
    }
}
