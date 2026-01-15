package com.clinic.backend.controller;

import com.clinic.backend.mapper.BillingMapper;
import com.clinic.backend.service.BillingService;
import com.clinic.common.dto.request.CreateBillingRequest;
import com.clinic.common.dto.request.RecordPaymentRequest;
import com.clinic.common.dto.response.BillingResponse;
import com.clinic.common.dto.view.BillingListViewDTO;
import com.clinic.common.dto.view.BillingSummaryViewDTO;
import com.clinic.common.dto.view.OverduePaymentViewDTO;
import com.clinic.common.entity.operational.Billing;
import com.clinic.backend.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Billing operations.
 * Implements CQRS pattern: READ operations use database views, WRITE operations use JPA entities.
 *
 * Endpoints:
 * READ (using views):
 *   GET  /api/billings                     - List all billings (v_billing_list)
 *   GET  /api/billings/{id}                - Get billing detail (v_billing_list)
 *   GET  /api/billings/patient/{patientId} - Get patient's billings
 *   GET  /api/billings/status/{status}     - Get by payment status
 *   GET  /api/billings/overdue             - Get overdue payments (v_overdue_payments)
 *   GET  /api/billings/summary/monthly     - Monthly summary (mv_billing_summary_by_period)
 *   GET  /api/billings/summary/current     - Current month summary
 *   GET  /api/billings/search              - Search billings
 *
 * WRITE (using entities):
 *   POST   /api/billings                   - Create billing
 *   PUT    /api/billings/{id}              - Update billing
 *   POST   /api/billings/{id}/payment      - Record payment
 *   POST   /api/billings/{id}/mark-paid    - Mark as fully paid
 *   POST   /api/billings/{id}/cancel       - Cancel billing
 *   DELETE /api/billings/{id}              - Soft delete
 */
@Slf4j
@RestController
@RequestMapping("/api/billings")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Billing and payment management endpoints")
public class BillingController {

    private final BillingService billingService;
    private final BillingMapper billingMapper;

    // ========================================================================
    // READ ENDPOINTS (using database views - CQRS Query side)
    // ========================================================================

    /**
     * Get all billings for current tenant using v_billing_list view.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "List all billings", description = "Get all billings for the current tenant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved billing list"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<List<BillingListViewDTO>> getAllBillings() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Getting all billings for tenant: {}", tenantId);
        List<BillingListViewDTO> billings = billingService.getBillingListView(tenantId);
        return ResponseEntity.ok(billings);
    }

    /**
     * Get billing by ID using v_billing_list view.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "Get billing details", description = "Get billing details by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved billing"),
            @ApiResponse(responseCode = "404", description = "Billing not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<BillingListViewDTO> getBillingById(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Getting billing: {} for tenant: {}", id, tenantId);
        return billingService.getBillingDetailView(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get billings for a specific patient.
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "Get patient billings", description = "Get all billings for a specific patient")
    public ResponseEntity<List<BillingListViewDTO>> getPatientBillings(@PathVariable UUID patientId) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Getting billings for patient: {}", patientId);
        List<BillingListViewDTO> billings = billingService.getPatientBillingsView(patientId, tenantId);
        return ResponseEntity.ok(billings);
    }

    /**
     * Get billings by payment status (PENDING, PAID, PARTIALLY_PAID, CANCELLED, etc.).
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "Get billings by payment status", description = "Filter billings by payment status")
    public ResponseEntity<List<BillingListViewDTO>> getBillingsByPaymentStatus(@PathVariable String status) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Getting billings with status: {}", status);
        List<BillingListViewDTO> billings = billingService.getBillingsByPaymentStatusView(tenantId, status.toUpperCase());
        return ResponseEntity.ok(billings);
    }

    /**
     * Get billings by billing status (FULLY_PAID, PARTIAL, OVERDUE, PENDING).
     */
    @GetMapping("/billing-status/{billingStatus}")
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "Get billings by billing status", description = "Filter billings by billing status")
    public ResponseEntity<List<BillingListViewDTO>> getBillingsByBillingStatus(@PathVariable String billingStatus) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Getting billings with billing status: {}", billingStatus);
        List<BillingListViewDTO> billings = billingService.getBillingsByBillingStatusView(tenantId, billingStatus.toUpperCase());
        return ResponseEntity.ok(billings);
    }

    /**
     * Get billings within a date range.
     */
    @GetMapping("/date-range")
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "Get billings by date range", description = "Get billings within a specified date range")
    public ResponseEntity<List<BillingListViewDTO>> getBillingsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Getting billings from {} to {}", startDate, endDate);
        List<BillingListViewDTO> billings = billingService.getBillingsByDateRangeView(tenantId, startDate, endDate);
        return ResponseEntity.ok(billings);
    }

    /**
     * Search billings by invoice number or patient name.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "Search billings", description = "Search billings by invoice number or patient name")
    public ResponseEntity<List<BillingListViewDTO>> searchBillings(@RequestParam String q) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Searching billings with term: {}", q);
        List<BillingListViewDTO> billings = billingService.searchBillingsView(tenantId, q);
        return ResponseEntity.ok(billings);
    }

    // ========================================================================
    // OVERDUE PAYMENTS (using v_overdue_payments view)
    // ========================================================================

    /**
     * Get all overdue payments using v_overdue_payments view.
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "Get overdue payments", description = "Get all overdue payments for the tenant")
    public ResponseEntity<List<OverduePaymentViewDTO>> getOverduePayments() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Getting overdue payments for tenant: {}", tenantId);
        List<OverduePaymentViewDTO> overduePayments = billingService.getOverduePaymentsView(tenantId);
        return ResponseEntity.ok(overduePayments);
    }

    /**
     * Get overdue payments by aging bucket.
     * Buckets: "0-30 days", "31-60 days", "61-90 days", "90+ days"
     */
    @GetMapping("/overdue/bucket/{bucket}")
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "Get overdue by aging bucket", description = "Get overdue payments filtered by aging bucket")
    public ResponseEntity<List<OverduePaymentViewDTO>> getOverdueByAgingBucket(@PathVariable String bucket) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Getting overdue payments for bucket: {}", bucket);
        List<OverduePaymentViewDTO> overduePayments = billingService.getOverdueByAgingBucketView(tenantId, bucket);
        return ResponseEntity.ok(overduePayments);
    }

    /**
     * Get overdue summary (total amount and counts by aging bucket).
     */
    @GetMapping("/overdue/summary")
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "Get overdue summary", description = "Get total overdue amount and counts by aging bucket")
    public ResponseEntity<Map<String, Object>> getOverdueSummary() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        BigDecimal totalOverdue = billingService.getTotalOverdueAmountView(tenantId);
        List<Object[]> bucketCounts = billingService.getOverdueCountByAgingBucket(tenantId);

        return ResponseEntity.ok(Map.of(
                "totalOverdueAmount", totalOverdue,
                "buckets", bucketCounts
        ));
    }

    // ========================================================================
    // BILLING SUMMARY (using mv_billing_summary_by_period materialized view)
    // ========================================================================

    /**
     * Get monthly billing summary for a specific month.
     */
    @GetMapping("/summary/monthly")
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "Get monthly summary", description = "Get billing summary for a specific month")
    public ResponseEntity<List<BillingSummaryViewDTO>> getMonthlySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Getting monthly summary for: {}", month);
        List<BillingSummaryViewDTO> summary = billingService.getMonthlySummaryView(tenantId, month);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get billing summary for a range of months.
     */
    @GetMapping("/summary/range")
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "Get summary by month range", description = "Get billing summary for a range of months")
    public ResponseEntity<List<BillingSummaryViewDTO>> getSummaryByMonthRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startMonth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endMonth) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Getting summary from {} to {}", startMonth, endMonth);
        List<BillingSummaryViewDTO> summary = billingService.getSummaryByMonthRangeView(tenantId, startMonth, endMonth);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get yearly billing summary.
     */
    @GetMapping("/summary/yearly/{year}")
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "Get yearly summary", description = "Get billing summary for a specific year")
    public ResponseEntity<List<BillingSummaryViewDTO>> getYearlySummary(@PathVariable int year) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Getting yearly summary for: {}", year);
        List<BillingSummaryViewDTO> summary = billingService.getYearlySummaryView(tenantId, year);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get daily revenue for a month (for charts).
     */
    @GetMapping("/summary/daily")
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "Get daily revenue", description = "Get daily revenue breakdown for a specific month")
    public ResponseEntity<List<BillingSummaryViewDTO>> getDailyRevenueForMonth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Getting daily revenue for month: {}", month);
        List<BillingSummaryViewDTO> dailyRevenue = billingService.getDailyRevenueForMonthView(tenantId, month);
        return ResponseEntity.ok(dailyRevenue);
    }

    /**
     * Get current month summary (dashboard widget).
     */
    @GetMapping("/summary/current")
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "Get current month summary", description = "Get billing summary for the current month (dashboard widget)")
    public ResponseEntity<BillingSummaryViewDTO> getCurrentMonthSummary() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Getting current month summary for tenant: {}", tenantId);
        return billingService.getCurrentMonthSummaryView(tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(BillingSummaryViewDTO.builder()
                        .tenantId(tenantId)
                        .totalInvoices(0L)
                        .paidInvoices(0L)
                        .pendingInvoices(0L)
                        .partialInvoices(0L)
                        .cancelledInvoices(0L)
                        .totalRevenue(BigDecimal.ZERO)
                        .collectedRevenue(BigDecimal.ZERO)
                        .outstandingBalance(BigDecimal.ZERO)
                        .build()));
    }

    // ========================================================================
    // WRITE ENDPOINTS (using JPA entities - CQRS Command side)
    // ========================================================================

    /**
     * Create a new billing record.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'BILLING_STAFF')")
    public ResponseEntity<BillingResponse> createBilling(@Valid @RequestBody CreateBillingRequest request) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Creating billing for patient: {}", request.getPatientId());

        Billing billing = billingMapper.toEntity(request);
        Billing created = billingService.createBilling(billing, tenantId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billingMapper.toResponse(created));
    }

    /**
     * Record a payment against a billing record.
     */
    @PostMapping("/{id}/payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'BILLING_STAFF')")
    public ResponseEntity<BillingResponse> recordPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RecordPaymentRequest request) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Recording payment of {} for billing: {}", request.getAmount(), id);

        Billing updated = billingService.recordPayment(
                id, tenantId, request.getAmount(), request.getPaymentMethod());

        return ResponseEntity.ok(billingMapper.toResponse(updated));
    }

    /**
     * Mark billing as fully paid (convenience endpoint).
     */
    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<BillingResponse> markAsPaid(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Marking billing as paid: {}", id);

        Billing updated = billingService.markAsPaid(id, tenantId);
        return ResponseEntity.ok(billingMapper.toResponse(updated));
    }

    /**
     * Cancel a billing record.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<BillingResponse> cancelBilling(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Cancelling billing: {}", id);

        Billing updated = billingService.cancelBilling(id, tenantId);
        return ResponseEntity.ok(billingMapper.toResponse(updated));
    }

    /**
     * Soft delete a billing record.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBilling(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        log.debug("Soft deleting billing: {}", id);

        billingService.softDeleteBilling(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // AGGREGATE QUERIES
    // ========================================================================

    /**
     * Get outstanding balance for a patient.
     */
    @GetMapping("/patient/{patientId}/outstanding")
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "Get patient outstanding balance", description = "Get outstanding balance for a specific patient")
    public ResponseEntity<Map<String, Object>> getPatientOutstandingBalance(@PathVariable UUID patientId) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        BigDecimal outstanding = billingService.getPatientOutstandingBalance(patientId, tenantId);
        return ResponseEntity.ok(Map.of(
                "patientId", patientId,
                "outstandingBalance", outstanding != null ? outstanding : BigDecimal.ZERO
        ));
    }

    /**
     * Get total outstanding balance for tenant.
     */
    @GetMapping("/outstanding/total")
    @PreAuthorize("hasAnyAuthority('BILLING_READ', 'ADMIN')")
    @Operation(summary = "Get tenant outstanding balance", description = "Get total outstanding balance for the tenant")
    public ResponseEntity<Map<String, Object>> getTenantOutstandingBalance() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        BigDecimal outstanding = billingService.getOutstandingBalanceForTenant(tenantId);
        return ResponseEntity.ok(Map.of(
                "tenantId", tenantId,
                "outstandingBalance", outstanding != null ? outstanding : BigDecimal.ZERO
        ));
    }
}
