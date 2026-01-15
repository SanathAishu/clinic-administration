package com.clinic.common.entity.patient;

import com.clinic.common.entity.core.User;
import com.clinic.common.entity.TenantAwareEntity;
import com.clinic.common.entity.clinical.MedicalRecord;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "diagnoses", indexes = {
    @Index(name = "idx_diagnoses_tenant", columnList = "tenant_id"),
    @Index(name = "idx_diagnoses_medical_record", columnList = "medical_record_id"),
    @Index(name = "idx_diagnoses_icd10", columnList = "icd10_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Diagnosis extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id", nullable = false)
    @NotNull
    private MedicalRecord medicalRecord;

    // Diagnosis Details
    @Column(name = "icd10_code", length = 10)
    @Size(max = 10)
    private String icd10Code;

    @Column(name = "diagnosis_name", nullable = false, length = 500)
    @NotBlank(message = "Diagnosis name is required")
    @Size(max = 500)
    private String diagnosisName;

    @Column(name = "diagnosis_type", nullable = false, length = 50)
    @NotBlank(message = "Diagnosis type is required")
    @Size(max = 50)
    private String diagnosisType;

    @Column(name = "severity", length = 50)
    @Size(max = 50)
    private String severity;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Metadata
    @Column(name = "diagnosed_at", nullable = false)
    @NotNull
    private LocalDate diagnosedAt = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @NotNull
    private User createdBy;
}
