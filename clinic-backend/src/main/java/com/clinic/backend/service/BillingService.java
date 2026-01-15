package com.clinic.backend.service;

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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingService {

    private final BillingRepository billingRepository;

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
}
