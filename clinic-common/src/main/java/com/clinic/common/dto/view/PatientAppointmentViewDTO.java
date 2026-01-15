package com.clinic.common.dto.view;

import com.clinic.common.enums.AppointmentStatus;
import com.clinic.common.enums.ConsultationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO mapping to v_patient_appointments database view.
 * Patient appointments with doctor details and related medical record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientAppointmentViewDTO {

    private UUID id;
    private UUID tenantId;
    private UUID patientId;
    private String patientName;
    private String patientPhone;

    // Doctor Info
    private UUID doctorId;
    private String doctorName;
    private String doctorEmail;

    // Appointment Info
    private LocalDateTime appointmentTime;
    private LocalDate appointmentDate;
    private String appointmentTimeOnly;
    private Integer durationMinutes;
    private LocalDateTime endTime;
    private ConsultationType consultationType;
    private AppointmentStatus status;
    private String reason;
    private String notes;

    // Status Timestamps
    private Instant confirmedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private String cancellationReason;

    // Metadata
    private Instant createdAt;
    private Instant updatedAt;

    // Related Medical Record
    private UUID medicalRecordId;
    private String chiefComplaint;

    // Computed Flags
    private Boolean isOverdue;
    private Boolean isToday;
}
