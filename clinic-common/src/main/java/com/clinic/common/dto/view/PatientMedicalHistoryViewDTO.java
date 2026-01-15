package com.clinic.common.dto.view;

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
 * DTO mapping to v_patient_medical_history database view.
 * Patient medical records with doctor info and related item counts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientMedicalHistoryViewDTO {

    private UUID id;
    private UUID tenantId;
    private UUID patientId;
    private String patientName;

    // Doctor Info
    private UUID doctorId;
    private String doctorName;

    // Medical Record Info
    private LocalDate recordDate;
    private String chiefComplaint;
    private String historyPresentIllness;
    private String examinationFindings;
    private String clinicalNotes;
    private String treatmentPlan;
    private String followUpInstructions;
    private LocalDate followUpDate;

    // Related Appointment
    private UUID appointmentId;
    private LocalDateTime appointmentTime;
    private ConsultationType consultationType;

    // Item Counts
    private Long diagnosisCount;
    private Long prescriptionCount;
    private Long labTestCount;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}
