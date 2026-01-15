package com.clinic.common.entity.clinical;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prescription_items", indexes = {
    @Index(name = "idx_prescription_items_prescription", columnList = "prescription_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItem {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    @NotNull
    private Prescription prescription;

    // Medication Details
    @Column(name = "medication_name", nullable = false)
    @NotBlank(message = "Medication name is required")
    @Size(max = 255)
    private String medicationName;

    @Column(name = "dosage", nullable = false, length = 100)
    @NotBlank(message = "Dosage is required")
    @Size(max = 100)
    private String dosage;

    @Column(name = "frequency", nullable = false, length = 100)
    @NotBlank(message = "Frequency is required")
    @Size(max = 100)
    private String frequency;

    @Column(name = "duration_days", nullable = false)
    @NotNull
    @Min(value = 1, message = "Duration must be at least 1 day")
    @Max(value = 365, message = "Duration cannot exceed 365 days")
    private Integer durationDays;

    @Column(name = "quantity", nullable = false)
    @NotNull
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    // Metadata
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
