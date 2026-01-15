package com.clinic.backend.service;

import com.clinic.common.entity.patient.Diagnosis;
import com.clinic.backend.repository.DiagnosisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;

    @Transactional
    public Diagnosis createDiagnosis(Diagnosis diagnosis, UUID tenantId) {
        log.debug("Creating diagnosis for medical record: {}", diagnosis.getMedicalRecord().getId());

        diagnosis.setTenantId(tenantId);

        if (diagnosis.getDiagnosedAt() == null) {
            diagnosis.setDiagnosedAt(LocalDate.now());
        }

        Diagnosis saved = diagnosisRepository.save(diagnosis);
        log.info("Created diagnosis: {}", saved.getId());
        return saved;
    }

    public Diagnosis getDiagnosisById(UUID id, UUID tenantId) {
        return diagnosisRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Diagnosis not found: " + id));
    }

    public Page<Diagnosis> getDiagnosesForMedicalRecord(UUID medicalRecordId, UUID tenantId, Pageable pageable) {
        return diagnosisRepository.findByMedicalRecordIdAndTenantId(medicalRecordId, tenantId, pageable);
    }

    public List<Diagnosis> getDiagnosesForPatient(UUID patientId, UUID tenantId) {
        return diagnosisRepository.findByPatientId(patientId, tenantId);
    }

    public List<Diagnosis> getDiagnosesByIcd10Code(String icd10Code, UUID tenantId) {
        return diagnosisRepository.findByIcd10CodeAndTenantId(icd10Code, tenantId);
    }

    public List<Diagnosis> getActiveDiagnosesForPatient(UUID patientId, UUID tenantId) {
        return diagnosisRepository.findActiveDiagnosesForPatient(patientId, tenantId);
    }

    @Transactional
    public Diagnosis updateDiagnosis(UUID id, UUID tenantId, Diagnosis updates) {
        Diagnosis diagnosis = getDiagnosisById(id, tenantId);

        if (updates.getIcd10Code() != null) diagnosis.setIcd10Code(updates.getIcd10Code());
        if (updates.getDiagnosisName() != null) diagnosis.setDiagnosisName(updates.getDiagnosisName());
        if (updates.getDiagnosisType() != null) diagnosis.setDiagnosisType(updates.getDiagnosisType());
        if (updates.getSeverity() != null) diagnosis.setSeverity(updates.getSeverity());
        if (updates.getNotes() != null) diagnosis.setNotes(updates.getNotes());
        if (updates.getDiagnosedAt() != null) diagnosis.setDiagnosedAt(updates.getDiagnosedAt());

        return diagnosisRepository.save(diagnosis);
    }

    @Transactional
    public void deleteDiagnosis(UUID id, UUID tenantId) {
        Diagnosis diagnosis = getDiagnosisById(id, tenantId);
        diagnosisRepository.delete(diagnosis);
        log.info("Deleted diagnosis: {}", id);
    }

    public long countDiagnosesForPatient(UUID patientId, UUID tenantId) {
        return diagnosisRepository.countByPatientId(patientId, tenantId);
    }
}
