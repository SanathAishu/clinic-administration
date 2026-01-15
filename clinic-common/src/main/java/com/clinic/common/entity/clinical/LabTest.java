package com.clinic.common.entity.clinical;

import com.clinic.common.entity.core.User;
import com.clinic.common.entity.SoftDeletableEntity;
import com.clinic.common.entity.patient.Patient;
import com.clinic.common.enums.LabTestStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lab_tests", indexes = {
    @Index(name = "idx_lab_tests_tenant", columnList = "tenant_id"),
    @Index(name = "idx_lab_tests_patient", columnList = "patient_id, ordered_at"),
    @Index(name = "idx_lab_tests_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabTest extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @NotNull
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id")
    private MedicalRecord medicalRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordered_by", nullable = false)
    @NotNull
    private User orderedBy;

    // Test Details
    @Column(name = "test_name", nullable = false)
    @NotBlank(message = "Test name is required")
    @Size(max = 255)
    private String testName;

    @Column(name = "test_code", length = 50)
    @Size(max = 50)
    private String testCode;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private LabTestStatus status = LabTestStatus.ORDERED;

    // Tracking
    @Column(name = "ordered_at", nullable = false)
    @NotNull
    private Instant orderedAt = Instant.now();

    @Column(name = "sample_collected_at")
    private Instant sampleCollectedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "labTest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LabResult> results = new ArrayList<>();

    // State machine methods
    public void markSampleCollected() {
        this.status = LabTestStatus.SAMPLE_COLLECTED;
        this.sampleCollectedAt = Instant.now();
    }

    public void markInProgress() {
        this.status = LabTestStatus.IN_PROGRESS;
    }

    public void complete() {
        this.status = LabTestStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void cancel() {
        this.status = LabTestStatus.CANCELLED;
    }

    // Helper methods
    public void addResult(LabResult result) {
        results.add(result);
        result.setLabTest(this);
    }
}
