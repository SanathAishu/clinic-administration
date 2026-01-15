package com.clinic.common.dto.view;

import com.clinic.common.enums.AppointmentStatus;
import com.clinic.common.enums.BloodGroup;
import com.clinic.common.enums.ConsultationType;
import com.clinic.common.enums.Gender;
import com.clinic.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO mapping to v_appointment_detail database view.
 * Complete appointment details with patient, doctor, medical record, and billing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDetailViewDTO {

    private UUID id;
    private UUID tenantId;

    // Patient Information
    private UUID patientId;
    private String patientFirstName;
    private String patientLastName;
    private String patientName;
    private String patientPhone;
    private String patientEmail;
    private LocalDate patientDob;
    private Integer patientAge;
    private Gender patientGender;
    private BloodGroup patientBloodGroup;
    private String patientAbhaId;

    // Doctor Information
    private UUID doctorId;
    private String doctorFirstName;
    private String doctorLastName;
    private String doctorName;
    private String doctorEmail;

    // Appointment Details
    private Instant appointmentTime;
    private LocalDate appointmentDate;
    private String appointmentTimeOnly;
    private Integer durationMinutes;
    private Instant endTime;
    private ConsultationType consultationType;
    private AppointmentStatus status;
    private String reason;
    private String notes;

    // State Machine Tracking
    private Instant confirmedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private UUID cancelledBy;
    private String cancelledByName;
    private String cancellationReason;

    // Associated Medical Record
    private UUID medicalRecordId;
    private String chiefComplaint;
    private String clinicalNotes;
    private String treatmentPlan;

    // Associated Billing
    private UUID billingId;
    private String invoiceNumber;
    private BigDecimal totalAmount;
    private BigDecimal balanceAmount;
    private PaymentStatus paymentStatus;

    // Metadata
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private String createdByName;
}
