package com.clinic.common.entity.clinical;

import com.clinic.common.entity.SoftDeletableEntity;
import com.clinic.common.entity.patient.Patient;
import com.clinic.common.entity.core.User;
import com.clinic.common.entity.patient.Patient;
import com.clinic.common.enums.AppointmentStatus;
import com.clinic.common.enums.ConsultationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appointments", indexes = {
    @Index(name = "idx_appointments_tenant", columnList = "tenant_id"),
    @Index(name = "idx_appointments_patient", columnList = "patient_id"),
    @Index(name = "idx_appointments_doctor_time", columnList = "doctor_id, appointment_time"),
    @Index(name = "idx_appointments_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @NotNull
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    @NotNull
    private User doctor;

    // Appointment Details
    @Column(name = "appointment_time", nullable = false)
    @NotNull(message = "Appointment time is required")
    @Future(message = "Appointment time must be in the future")
    private Instant appointmentTime;

    @Column(name = "duration_minutes", nullable = false)
    @NotNull
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Max(value = 240, message = "Duration cannot exceed 240 minutes")
    private Integer durationMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_type", nullable = false)
    @NotNull
    private ConsultationType consultationType = ConsultationType.IN_PERSON;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    // Additional Information
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // State Machine Tracking
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    // Metadata
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @NotNull
    private User createdBy;

    // State machine methods
    public void confirm() {
        this.status = AppointmentStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    public void start() {
        this.status = AppointmentStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    public void complete() {
        this.status = AppointmentStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void cancel(User user, String reason) {
        this.status = AppointmentStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelledBy = user;
        this.cancellationReason = reason;
    }

    public void markNoShow() {
        this.status = AppointmentStatus.NO_SHOW;
    }

    public boolean canBeCancelled() {
        return status == AppointmentStatus.SCHEDULED || status == AppointmentStatus.CONFIRMED;
    }

    public Instant getEndTime() {
        return appointmentTime.plusSeconds(durationMinutes * 60L);
    }
}
