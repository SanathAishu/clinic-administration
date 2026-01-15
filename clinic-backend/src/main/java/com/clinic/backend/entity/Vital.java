package com.clinic.backend.entity;

import com.clinic.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vitals", indexes = {
    @Index(name = "idx_vitals_tenant", columnList = "tenant_id"),
    @Index(name = "idx_vitals_patient", columnList = "patient_id, recorded_at"),
    @Index(name = "idx_vitals_appointment", columnList = "appointment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vital extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @NotNull
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    // Vital Signs
    @Column(name = "temperature_celsius", precision = 4, scale = 1)
    @DecimalMin(value = "30.0", message = "Temperature must be at least 30°C")
    @DecimalMax(value = "45.0", message = "Temperature cannot exceed 45°C")
    private BigDecimal temperatureCelsius;

    @Column(name = "pulse_bpm")
    @Min(value = 30, message = "Pulse must be at least 30 bpm")
    @Max(value = 250, message = "Pulse cannot exceed 250 bpm")
    private Integer pulseBpm;

    @Column(name = "systolic_bp")
    @Min(value = 50, message = "Systolic BP must be at least 50")
    @Max(value = 250, message = "Systolic BP cannot exceed 250")
    private Integer systolicBp;

    @Column(name = "diastolic_bp")
    @Min(value = 30, message = "Diastolic BP must be at least 30")
    @Max(value = 150, message = "Diastolic BP cannot exceed 150")
    private Integer diastolicBp;

    @Column(name = "respiratory_rate")
    @Min(value = 0, message = "Respiratory rate cannot be negative")
    private Integer respiratoryRate;

    @Column(name = "oxygen_saturation")
    @Min(value = 0, message = "Oxygen saturation cannot be negative")
    @Max(value = 100, message = "Oxygen saturation cannot exceed 100")
    private Integer oxygenSaturation;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    @DecimalMin(value = "0.0", message = "Weight cannot be negative")
    private BigDecimal weightKg;

    @Column(name = "height_cm", precision = 5, scale = 2)
    @DecimalMin(value = "0.0", message = "Height cannot be negative")
    private BigDecimal heightCm;

    @Column(name = "bmi", precision = 4, scale = 2)
    private BigDecimal bmi;

    // Additional Measurements
    @Column(name = "blood_glucose_mgdl")
    @Min(value = 0, message = "Blood glucose cannot be negative")
    private Integer bloodGlucoseMgdl;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Metadata
    @Column(name = "recorded_at", nullable = false)
    @NotNull
    private Instant recordedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", nullable = false)
    @NotNull
    private User recordedBy;

    // Calculate BMI if weight and height are available
    public void calculateBmi() {
        if (weightKg != null && heightCm != null && heightCm.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal heightInMeters = heightCm.divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            this.bmi = weightKg.divide(
                heightInMeters.multiply(heightInMeters),
                2,
                BigDecimal.ROUND_HALF_UP
            );
        }
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        calculateBmi();
    }
}
