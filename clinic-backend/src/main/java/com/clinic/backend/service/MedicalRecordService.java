package com.clinic.backend.service;

import com.clinic.common.entity.clinical.MedicalRecord;
import com.clinic.backend.repository.MedicalRecordRepository;
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
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;

    @Transactional
    public MedicalRecord createMedicalRecord(MedicalRecord medicalRecord, UUID tenantId) {
        log.debug("Creating medical record for patient: {}", medicalRecord.getPatient().getId());

        medicalRecord.setTenantId(tenantId);

        if (medicalRecord.getRecordDate() == null) {
            medicalRecord.setRecordDate(LocalDate.now());
        }

        MedicalRecord saved = medicalRecordRepository.save(medicalRecord);
        log.info("Created medical record: {}", saved.getId());
        return saved;
    }

    public MedicalRecord getMedicalRecordById(UUID id, UUID tenantId) {
        return medicalRecordRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Medical record not found: " + id));
    }

    public Page<MedicalRecord> getMedicalRecordsForPatient(UUID patientId, UUID tenantId, Pageable pageable) {
        return medicalRecordRepository.findByPatientIdAndTenantIdAndDeletedAtIsNull(patientId, tenantId, pageable);
    }

    public List<MedicalRecord> getMedicalRecordsByDoctor(UUID doctorId, UUID tenantId) {
        return medicalRecordRepository.findByDoctorIdAndTenantId(doctorId, tenantId);
    }

    public List<MedicalRecord> getMedicalRecordsForAppointment(UUID appointmentId, UUID tenantId) {
        return medicalRecordRepository.findByAppointmentIdAndTenantId(appointmentId, tenantId);
    }

    public List<MedicalRecord> getRecentMedicalRecordsForPatient(UUID patientId, UUID tenantId, LocalDate since) {
        return medicalRecordRepository.findRecentRecordsForPatient(patientId, tenantId, since);
    }

    @Transactional
    public MedicalRecord updateMedicalRecord(UUID id, UUID tenantId, MedicalRecord updates) {
        MedicalRecord medicalRecord = getMedicalRecordById(id, tenantId);

        if (updates.getChiefComplaint() != null) medicalRecord.setChiefComplaint(updates.getChiefComplaint());
        if (updates.getHistoryPresentIllness() != null) medicalRecord.setHistoryPresentIllness(updates.getHistoryPresentIllness());
        if (updates.getExaminationFindings() != null) medicalRecord.setExaminationFindings(updates.getExaminationFindings());
        if (updates.getClinicalNotes() != null) medicalRecord.setClinicalNotes(updates.getClinicalNotes());
        if (updates.getTreatmentPlan() != null) medicalRecord.setTreatmentPlan(updates.getTreatmentPlan());
        if (updates.getFollowUpInstructions() != null) medicalRecord.setFollowUpInstructions(updates.getFollowUpInstructions());
        if (updates.getFollowUpDate() != null) medicalRecord.setFollowUpDate(updates.getFollowUpDate());

        return medicalRecordRepository.save(medicalRecord);
    }

    @Transactional
    public void softDeleteMedicalRecord(UUID id, UUID tenantId) {
        MedicalRecord medicalRecord = getMedicalRecordById(id, tenantId);
        medicalRecord.softDelete();
        medicalRecordRepository.save(medicalRecord);
        log.info("Soft deleted medical record: {}", id);
    }

    public long countMedicalRecordsForPatient(UUID patientId, UUID tenantId) {
        return medicalRecordRepository.countByPatientIdAndTenantId(patientId, tenantId);
    }
}
