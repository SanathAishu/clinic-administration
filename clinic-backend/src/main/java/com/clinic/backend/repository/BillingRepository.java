package com.clinic.backend.repository;

import com.clinic.common.entity.operational.Billing;
import com.clinic.common.enums.PaymentMethod;
import com.clinic.common.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillingRepository extends JpaRepository<Billing, UUID> {

    // Tenant-scoped queries
    Page<Billing> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<Billing> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // Invoice number lookup (unique per tenant)
    Optional<Billing> findByInvoiceNumberAndTenantId(String invoiceNumber, UUID tenantId);

    boolean existsByInvoiceNumberAndTenantId(String invoiceNumber, UUID tenantId);

    // Patient billing
    @Query("SELECT b FROM Billing b WHERE b.patient.id = :patientId AND b.tenantId = :tenantId AND " +
           "b.deletedAt IS NULL ORDER BY b.invoiceDate DESC")
    Page<Billing> findPatientBillings(@Param("patientId") UUID patientId,
                                       @Param("tenantId") UUID tenantId,
                                       Pageable pageable);

    @Query("SELECT b FROM Billing b WHERE b.patient.id = :patientId AND b.tenantId = :tenantId AND " +
           "b.deletedAt IS NULL ORDER BY b.invoiceDate DESC")
    Page<Billing> findByPatientIdAndTenantIdAndDeletedAtIsNull(@Param("patientId") UUID patientId,
                                                                @Param("tenantId") UUID tenantId,
                                                                Pageable pageable);

    // Appointment billing
    Optional<Billing> findByAppointmentIdAndTenantIdAndDeletedAtIsNull(UUID appointmentId, UUID tenantId);

    // Payment status queries (Invariant: balance = total - paid)
    List<Billing> findByTenantIdAndPaymentStatusAndDeletedAtIsNull(UUID tenantId, PaymentStatus status);

    @Query("SELECT b FROM Billing b WHERE b.tenantId = :tenantId AND b.paymentStatus = :status AND b.deletedAt IS NULL")
    List<Billing> findByTenantIdAndPaymentStatus(@Param("tenantId") UUID tenantId, @Param("status") PaymentStatus status);

    Page<Billing> findByTenantIdAndPaymentStatusInAndDeletedAtIsNull(UUID tenantId, List<PaymentStatus> statuses, Pageable pageable);

    // Pending payments
    @Query("SELECT b FROM Billing b WHERE b.tenantId = :tenantId AND b.paymentStatus IN ('PENDING', 'PARTIAL') AND " +
           "b.deletedAt IS NULL ORDER BY b.invoiceDate ASC")
    List<Billing> findPendingPayments(@Param("tenantId") UUID tenantId);

    @Query("SELECT b FROM Billing b WHERE b.patient.id = :patientId AND b.tenantId = :tenantId AND " +
           "b.paymentStatus IN ('PENDING', 'PARTIAL') AND b.deletedAt IS NULL ORDER BY b.invoiceDate ASC")
    List<Billing> findPendingBillingsForPatient(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);

    // Overdue payments
    @Query("SELECT b FROM Billing b WHERE b.tenantId = :tenantId AND b.paymentStatus IN ('PENDING', 'PARTIAL') AND " +
           "b.dueDate < :today AND b.deletedAt IS NULL ORDER BY b.dueDate ASC")
    List<Billing> findOverduePayments(@Param("tenantId") UUID tenantId, @Param("today") LocalDate today);

    @Query("SELECT b FROM Billing b WHERE b.tenantId = :tenantId AND b.paymentStatus IN ('PENDING', 'PARTIAL') AND " +
           "b.dueDate < :dueDate AND b.deletedAt IS NULL ORDER BY b.dueDate ASC")
    List<Billing> findOverdueBillings(@Param("tenantId") UUID tenantId, @Param("dueDate") LocalDate dueDate);

    // Payment method queries
    List<Billing> findByTenantIdAndPaymentMethodAndDeletedAtIsNull(UUID tenantId, PaymentMethod paymentMethod);

    // Date range queries
    @Query("SELECT b FROM Billing b WHERE b.tenantId = :tenantId AND " +
           "b.invoiceDate BETWEEN :startDate AND :endDate AND b.deletedAt IS NULL ORDER BY b.invoiceDate DESC")
    List<Billing> findByInvoiceDateRange(@Param("tenantId") UUID tenantId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    // Financial analytics
    @Query("SELECT SUM(b.totalAmount) FROM Billing b WHERE b.tenantId = :tenantId AND " +
           "b.invoiceDate BETWEEN :startDate AND :endDate AND b.deletedAt IS NULL")
    BigDecimal calculateTotalRevenue(@Param("tenantId") UUID tenantId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(b.paidAmount) FROM Billing b WHERE b.tenantId = :tenantId AND " +
           "b.invoiceDate BETWEEN :startDate AND :endDate AND b.deletedAt IS NULL")
    BigDecimal calculateCollectedRevenue(@Param("tenantId") UUID tenantId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(b.balanceAmount) FROM Billing b WHERE b.tenantId = :tenantId AND " +
           "b.paymentStatus IN ('PENDING', 'PARTIALLY_PAID') AND b.deletedAt IS NULL")
    BigDecimal calculateOutstandingBalance(@Param("tenantId") UUID tenantId);

    // Patient outstanding balance
    @Query("SELECT SUM(b.balanceAmount) FROM Billing b WHERE b.patient.id = :patientId AND b.tenantId = :tenantId AND " +
           "b.paymentStatus IN ('PENDING', 'PARTIALLY_PAID') AND b.deletedAt IS NULL")
    BigDecimal calculatePatientOutstandingBalance(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);

    // Counting
    long countByTenantIdAndPaymentStatusAndDeletedAtIsNull(UUID tenantId, PaymentStatus status);

    long countByPatientIdAndTenantIdAndDeletedAtIsNull(UUID patientId, UUID tenantId);
}
