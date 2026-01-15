package com.clinic.common.entity.clinical;

import com.clinic.common.entity.core.User;
import com.clinic.common.entity.SoftDeletableEntity;
import com.clinic.common.entity.clinical.Appointment;
import com.clinic.common.entity.patient.Patient;
import com.clinic.common.entity.clinical.Appointment;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "medical_records", indexes = {
    @Index(name = "idx_medical_records_tenant", columnList = "tenant_id"),
    @Index(name = "idx_medical_records_patient", columnList = "patient_id, record_date"),
    @Index(name = "idx_medical_records_appointment", columnList = "appointment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecord extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @NotNull
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    @NotNull
    private User doctor;

    // Clinical Documentation
    @Column(name = "chief_complaint", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Chief complaint is required")
    private String chiefComplaint;

    @Column(name = "history_present_illness", columnDefinition = "TEXT")
    private String historyPresentIllness;

    @Column(name = "examination_findings", columnDefinition = "TEXT")
    private String examinationFindings;

    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes;

    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan;

    @Column(name = "follow_up_instructions", columnDefinition = "TEXT")
    private String followUpInstructions;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    // Metadata
    @Column(name = "record_date", nullable = false)
    @NotNull
    private LocalDate recordDate = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @NotNull
    private User createdBy;
}
