package com.clinic.backend.repository;

import com.clinic.common.dto.view.BillingListViewDTO;
import com.clinic.common.dto.view.BillingSummaryViewDTO;
import com.clinic.common.dto.view.OverduePaymentViewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for billing-related database views (CQRS Read side).
 * Uses native SQL queries against v_billing_list, v_overdue_payments,
 * and mv_billing_summary_by_period views.
 */
@Repository
@RequiredArgsConstructor
public class BillingViewRepository {

    private final JdbcTemplate jdbcTemplate;

    // ========================================================================
    // ROW MAPPERS
    // ========================================================================

    private final RowMapper<BillingListViewDTO> billingListMapper = (rs, rowNum) ->
            BillingListViewDTO.builder()
                    .id(UUID.fromString(rs.getString("id")))
                    .tenantId(UUID.fromString(rs.getString("tenant_id")))
                    .patientId(UUID.fromString(rs.getString("patient_id")))
                    .patientName(rs.getString("patient_name"))
                    .patientPhone(rs.getString("patient_phone"))
                    .invoiceNumber(rs.getString("invoice_number"))
                    .invoiceDate(rs.getDate("invoice_date") != null ? rs.getDate("invoice_date").toLocalDate() : null)
                    .subtotal(rs.getBigDecimal("subtotal"))
                    .discountAmount(rs.getBigDecimal("discount_amount"))
                    .taxAmount(rs.getBigDecimal("tax_amount"))
                    .totalAmount(rs.getBigDecimal("total_amount"))
                    .paidAmount(rs.getBigDecimal("paid_amount"))
                    .balanceAmount(rs.getBigDecimal("balance_amount"))
                    .paymentStatus(rs.getString("payment_status"))
                    .paymentMethod(rs.getString("payment_method"))
                    .paymentDate(rs.getDate("payment_date") != null ? rs.getDate("payment_date").toLocalDate() : null)
                    .appointmentId(rs.getString("appointment_id") != null ? UUID.fromString(rs.getString("appointment_id")) : null)
                    .appointmentTime(rs.getTimestamp("appointment_time") != null ? rs.getTimestamp("appointment_time").toInstant() : null)
                    .doctorName(rs.getString("doctor_name"))
                    .billingStatus(rs.getString("billing_status"))
                    .daysOverdue(rs.getInt("days_overdue"))
                    .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null)
                    .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null)
                    .build();

    private final RowMapper<OverduePaymentViewDTO> overduePaymentMapper = (rs, rowNum) ->
            OverduePaymentViewDTO.builder()
                    .id(UUID.fromString(rs.getString("id")))
                    .tenantId(UUID.fromString(rs.getString("tenant_id")))
                    .patientId(UUID.fromString(rs.getString("patient_id")))
                    .patientName(rs.getString("patient_name"))
                    .patientPhone(rs.getString("patient_phone"))
                    .patientEmail(rs.getString("patient_email"))
                    .invoiceNumber(rs.getString("invoice_number"))
                    .invoiceDate(rs.getDate("invoice_date") != null ? rs.getDate("invoice_date").toLocalDate() : null)
                    .totalAmount(rs.getBigDecimal("total_amount"))
                    .paidAmount(rs.getBigDecimal("paid_amount"))
                    .balanceAmount(rs.getBigDecimal("balance_amount"))
                    .paymentStatus(rs.getString("payment_status"))
                    .daysOverdue(rs.getInt("days_overdue"))
                    .agingBucket(rs.getString("aging_bucket"))
                    .priorityScore(rs.getBigDecimal("priority_score"))
                    .build();

    private final RowMapper<BillingSummaryViewDTO> billingSummaryMapper = (rs, rowNum) ->
            BillingSummaryViewDTO.builder()
                    .tenantId(UUID.fromString(rs.getString("tenant_id")))
                    .periodDay(rs.getDate("period_day") != null ? rs.getDate("period_day").toLocalDate() : null)
                    .periodWeek(rs.getDate("period_week") != null ? rs.getDate("period_week").toLocalDate() : null)
                    .periodMonth(rs.getDate("period_month") != null ? rs.getDate("period_month").toLocalDate() : null)
                    .periodYear(rs.getDate("period_year") != null ? rs.getDate("period_year").toLocalDate() : null)
                    .totalInvoices(rs.getLong("total_invoices"))
                    .paidInvoices(rs.getLong("paid_invoices"))
                    .pendingInvoices(rs.getLong("pending_invoices"))
                    .partialInvoices(rs.getLong("partial_invoices"))
                    .cancelledInvoices(rs.getLong("cancelled_invoices"))
                    .totalRevenue(rs.getBigDecimal("total_revenue"))
                    .collectedRevenue(rs.getBigDecimal("collected_revenue"))
                    .outstandingBalance(rs.getBigDecimal("outstanding_balance"))
                    .cashPayments(rs.getLong("cash_payments"))
                    .cardPayments(rs.getLong("card_payments"))
                    .upiPayments(rs.getLong("upi_payments"))
                    .insurancePayments(rs.getLong("insurance_payments"))
                    .cashRevenue(rs.getBigDecimal("cash_revenue"))
                    .cardRevenue(rs.getBigDecimal("card_revenue"))
                    .upiRevenue(rs.getBigDecimal("upi_revenue"))
                    .insuranceRevenue(rs.getBigDecimal("insurance_revenue"))
                    .build();

    // ========================================================================
    // BILLING LIST VIEW QUERIES
    // ========================================================================

    /**
     * Get all billings for tenant using v_billing_list view.
     */
    public List<BillingListViewDTO> findAllByTenantId(UUID tenantId) {
        String sql = """
            SELECT * FROM v_billing_list
            WHERE tenant_id = ?
            ORDER BY invoice_date DESC, created_at DESC
            """;
        return jdbcTemplate.query(sql, billingListMapper, tenantId.toString());
    }

    /**
     * Get billing by ID using v_billing_list view.
     */
    public Optional<BillingListViewDTO> findById(UUID id, UUID tenantId) {
        String sql = """
            SELECT * FROM v_billing_list
            WHERE id = ? AND tenant_id = ?
            """;
        List<BillingListViewDTO> results = jdbcTemplate.query(sql, billingListMapper, id.toString(), tenantId.toString());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Get billings for a specific patient.
     */
    public List<BillingListViewDTO> findByPatientId(UUID patientId, UUID tenantId) {
        String sql = """
            SELECT * FROM v_billing_list
            WHERE patient_id = ? AND tenant_id = ?
            ORDER BY invoice_date DESC
            """;
        return jdbcTemplate.query(sql, billingListMapper, patientId.toString(), tenantId.toString());
    }

    /**
     * Get billings by payment status.
     */
    public List<BillingListViewDTO> findByPaymentStatus(UUID tenantId, String status) {
        String sql = """
            SELECT * FROM v_billing_list
            WHERE tenant_id = ? AND payment_status = ?
            ORDER BY invoice_date DESC
            """;
        return jdbcTemplate.query(sql, billingListMapper, tenantId.toString(), status);
    }

    /**
     * Get billings by billing status (FULLY_PAID, PARTIAL, OVERDUE, PENDING).
     */
    public List<BillingListViewDTO> findByBillingStatus(UUID tenantId, String billingStatus) {
        String sql = """
            SELECT * FROM v_billing_list
            WHERE tenant_id = ? AND billing_status = ?
            ORDER BY invoice_date DESC
            """;
        return jdbcTemplate.query(sql, billingListMapper, tenantId.toString(), billingStatus);
    }

    /**
     * Get billings within a date range.
     */
    public List<BillingListViewDTO> findByDateRange(UUID tenantId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT * FROM v_billing_list
            WHERE tenant_id = ? AND invoice_date BETWEEN ? AND ?
            ORDER BY invoice_date DESC
            """;
        return jdbcTemplate.query(sql, billingListMapper,
                tenantId.toString(), Date.valueOf(startDate), Date.valueOf(endDate));
    }

    /**
     * Search billings by invoice number or patient name.
     */
    public List<BillingListViewDTO> search(UUID tenantId, String searchTerm) {
        String sql = """
            SELECT * FROM v_billing_list
            WHERE tenant_id = ?
              AND (invoice_number ILIKE ? OR patient_name ILIKE ?)
            ORDER BY invoice_date DESC
            """;
        String pattern = "%" + searchTerm + "%";
        return jdbcTemplate.query(sql, billingListMapper, tenantId.toString(), pattern, pattern);
    }

    // ========================================================================
    // OVERDUE PAYMENTS VIEW QUERIES
    // ========================================================================

    /**
     * Get all overdue payments using v_overdue_payments view.
     */
    public List<OverduePaymentViewDTO> findOverduePayments(UUID tenantId) {
        String sql = """
            SELECT * FROM v_overdue_payments
            WHERE tenant_id = ?
            ORDER BY priority_score DESC
            """;
        return jdbcTemplate.query(sql, overduePaymentMapper, tenantId.toString());
    }

    /**
     * Get overdue payments by aging bucket.
     */
    public List<OverduePaymentViewDTO> findOverdueByAgingBucket(UUID tenantId, String agingBucket) {
        String sql = """
            SELECT * FROM v_overdue_payments
            WHERE tenant_id = ? AND aging_bucket = ?
            ORDER BY priority_score DESC
            """;
        return jdbcTemplate.query(sql, overduePaymentMapper, tenantId.toString(), agingBucket);
    }

    /**
     * Get total overdue amount for tenant.
     */
    public BigDecimal getTotalOverdueAmount(UUID tenantId) {
        String sql = """
            SELECT COALESCE(SUM(balance_amount), 0) FROM v_overdue_payments
            WHERE tenant_id = ?
            """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, tenantId.toString());
    }

    /**
     * Get overdue count by aging bucket.
     */
    public List<Object[]> getOverdueCountByAgingBucket(UUID tenantId) {
        String sql = """
            SELECT aging_bucket, COUNT(*), SUM(balance_amount)
            FROM v_overdue_payments
            WHERE tenant_id = ?
            GROUP BY aging_bucket
            ORDER BY aging_bucket
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new Object[]{
                rs.getString("aging_bucket"),
                rs.getLong(2),
                rs.getBigDecimal(3)
        }, tenantId.toString());
    }

    // ========================================================================
    // BILLING SUMMARY MATERIALIZED VIEW QUERIES
    // ========================================================================

    /**
     * Get billing summary for a specific month.
     */
    public List<BillingSummaryViewDTO> findMonthlySummary(UUID tenantId, LocalDate month) {
        String sql = """
            SELECT * FROM mv_billing_summary_by_period
            WHERE tenant_id = ? AND period_month = ?
            """;
        return jdbcTemplate.query(sql, billingSummaryMapper,
                tenantId.toString(), Date.valueOf(month.withDayOfMonth(1)));
    }

    /**
     * Get billing summary for date range (monthly aggregation).
     */
    public List<BillingSummaryViewDTO> findSummaryByMonthRange(UUID tenantId, LocalDate startMonth, LocalDate endMonth) {
        String sql = """
            SELECT
                tenant_id,
                NULL as period_day,
                NULL as period_week,
                period_month,
                NULL as period_year,
                SUM(total_invoices) as total_invoices,
                SUM(paid_invoices) as paid_invoices,
                SUM(pending_invoices) as pending_invoices,
                SUM(partial_invoices) as partial_invoices,
                SUM(cancelled_invoices) as cancelled_invoices,
                SUM(total_revenue) as total_revenue,
                SUM(collected_revenue) as collected_revenue,
                SUM(outstanding_balance) as outstanding_balance,
                SUM(cash_payments) as cash_payments,
                SUM(card_payments) as card_payments,
                SUM(upi_payments) as upi_payments,
                SUM(insurance_payments) as insurance_payments,
                SUM(cash_revenue) as cash_revenue,
                SUM(card_revenue) as card_revenue,
                SUM(upi_revenue) as upi_revenue,
                SUM(insurance_revenue) as insurance_revenue
            FROM mv_billing_summary_by_period
            WHERE tenant_id = ? AND period_month BETWEEN ? AND ?
            GROUP BY tenant_id, period_month
            ORDER BY period_month DESC
            """;
        return jdbcTemplate.query(sql, billingSummaryMapper,
                tenantId.toString(),
                Date.valueOf(startMonth.withDayOfMonth(1)),
                Date.valueOf(endMonth.withDayOfMonth(1)));
    }

    /**
     * Get yearly billing summary.
     */
    public List<BillingSummaryViewDTO> findYearlySummary(UUID tenantId, int year) {
        String sql = """
            SELECT
                tenant_id,
                NULL as period_day,
                NULL as period_week,
                NULL as period_month,
                period_year,
                SUM(total_invoices) as total_invoices,
                SUM(paid_invoices) as paid_invoices,
                SUM(pending_invoices) as pending_invoices,
                SUM(partial_invoices) as partial_invoices,
                SUM(cancelled_invoices) as cancelled_invoices,
                SUM(total_revenue) as total_revenue,
                SUM(collected_revenue) as collected_revenue,
                SUM(outstanding_balance) as outstanding_balance,
                SUM(cash_payments) as cash_payments,
                SUM(card_payments) as card_payments,
                SUM(upi_payments) as upi_payments,
                SUM(insurance_payments) as insurance_payments,
                SUM(cash_revenue) as cash_revenue,
                SUM(card_revenue) as card_revenue,
                SUM(upi_revenue) as upi_revenue,
                SUM(insurance_revenue) as insurance_revenue
            FROM mv_billing_summary_by_period
            WHERE tenant_id = ? AND EXTRACT(YEAR FROM period_year) = ?
            GROUP BY tenant_id, period_year
            """;
        return jdbcTemplate.query(sql, billingSummaryMapper, tenantId.toString(), year);
    }

    /**
     * Get daily revenue for a specific month (for charts).
     */
    public List<BillingSummaryViewDTO> findDailyRevenueForMonth(UUID tenantId, LocalDate month) {
        String sql = """
            SELECT * FROM mv_billing_summary_by_period
            WHERE tenant_id = ?
              AND period_month = ?
            ORDER BY period_day ASC
            """;
        return jdbcTemplate.query(sql, billingSummaryMapper,
                tenantId.toString(), Date.valueOf(month.withDayOfMonth(1)));
    }

    /**
     * Get aggregated totals for current month.
     */
    public Optional<BillingSummaryViewDTO> findCurrentMonthSummary(UUID tenantId) {
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        String sql = """
            SELECT
                tenant_id,
                NULL as period_day,
                NULL as period_week,
                ? as period_month,
                NULL as period_year,
                COALESCE(SUM(total_invoices), 0) as total_invoices,
                COALESCE(SUM(paid_invoices), 0) as paid_invoices,
                COALESCE(SUM(pending_invoices), 0) as pending_invoices,
                COALESCE(SUM(partial_invoices), 0) as partial_invoices,
                COALESCE(SUM(cancelled_invoices), 0) as cancelled_invoices,
                COALESCE(SUM(total_revenue), 0) as total_revenue,
                COALESCE(SUM(collected_revenue), 0) as collected_revenue,
                COALESCE(SUM(outstanding_balance), 0) as outstanding_balance,
                COALESCE(SUM(cash_payments), 0) as cash_payments,
                COALESCE(SUM(card_payments), 0) as card_payments,
                COALESCE(SUM(upi_payments), 0) as upi_payments,
                COALESCE(SUM(insurance_payments), 0) as insurance_payments,
                COALESCE(SUM(cash_revenue), 0) as cash_revenue,
                COALESCE(SUM(card_revenue), 0) as card_revenue,
                COALESCE(SUM(upi_revenue), 0) as upi_revenue,
                COALESCE(SUM(insurance_revenue), 0) as insurance_revenue
            FROM mv_billing_summary_by_period
            WHERE tenant_id = ? AND period_month = ?
            GROUP BY tenant_id
            """;
        List<BillingSummaryViewDTO> results = jdbcTemplate.query(sql, billingSummaryMapper,
                Date.valueOf(currentMonth), tenantId.toString(), Date.valueOf(currentMonth));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
