package com.clinic.common.dto.view;

import com.clinic.common.enums.ConsultationType;
import com.clinic.common.enums.PaymentMethod;
import com.clinic.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO mapping to v_patient_billing_history database view.
 * Patient billing records with payment status and appointment details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientBillingHistoryViewDTO {

    private UUID id;
    private UUID tenantId;
    private UUID patientId;
    private String patientName;
    private String patientPhone;

    // Invoice Info
    private String invoiceNumber;
    private LocalDate invoiceDate;

    // Financial Info
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;

    // Payment Info
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
    private String paymentReference;

    // Line Items (JSONB)
    private String lineItems;

    // Related Appointment
    private UUID appointmentId;
    private LocalDateTime appointmentTime;
    private ConsultationType consultationType;

    // Doctor Info
    private UUID doctorId;
    private String doctorName;

    // Computed Flags
    private Boolean isOverdue;
    private Boolean hasBalance;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}
