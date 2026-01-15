package com.clinic.backend.service;

import com.clinic.common.entity.clinical.Prescription;
import com.clinic.backend.repository.PrescriptionRepository;
import com.clinic.common.enums.PrescriptionStatus;
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
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;

    @Transactional
    public Prescription createPrescription(Prescription prescription, UUID tenantId) {
        log.debug("Creating prescription for patient: {}", prescription.getPatient().getId());

        prescription.setTenantId(tenantId);

        if (prescription.getPrescriptionDate() == null) {
            prescription.setPrescriptionDate(LocalDate.now());
        }

        if (prescription.getStatus() == null) {
            prescription.setStatus(PrescriptionStatus.ACTIVE);
        }

        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Created prescription: {}", saved.getId());
        return saved;
    }

    public Prescription getPrescriptionById(UUID id, UUID tenantId) {
        return prescriptionRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found: " + id));
    }

    public Page<Prescription> getPrescriptionsForPatient(UUID patientId, UUID tenantId, Pageable pageable) {
        return prescriptionRepository.findByPatientIdAndTenantIdAndDeletedAtIsNull(patientId, tenantId, pageable);
    }

    public List<Prescription> getPrescriptionsByDoctor(UUID doctorId, UUID tenantId) {
        return prescriptionRepository.findByDoctorIdAndTenantId(doctorId, tenantId);
    }

    public List<Prescription> getActivePrescriptionsForPatient(UUID patientId, UUID tenantId) {
        return prescriptionRepository.findActivePrescriptionsForPatient(patientId, tenantId, LocalDate.now());
    }

    public List<Prescription> getPrescriptionsByStatus(UUID tenantId, PrescriptionStatus status) {
        return prescriptionRepository.findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status);
    }

    @Transactional
    public Prescription dispensePrescription(UUID id, UUID tenantId) {
        Prescription prescription = getPrescriptionById(id, tenantId);

        if (prescription.getStatus() != PrescriptionStatus.ACTIVE) {
            throw new IllegalStateException("Can only dispense active prescriptions");
        }

        // Mark as COMPLETED when dispensed
        prescription.setStatus(PrescriptionStatus.COMPLETED);
        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Dispensed prescription: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Prescription completePrescription(UUID id, UUID tenantId) {
        Prescription prescription = getPrescriptionById(id, tenantId);
        prescription.setStatus(PrescriptionStatus.COMPLETED);
        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Completed prescription: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Prescription cancelPrescription(UUID id, UUID tenantId) {
        Prescription prescription = getPrescriptionById(id, tenantId);
        prescription.setStatus(PrescriptionStatus.CANCELLED);
        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Cancelled prescription: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Prescription updatePrescription(UUID id, UUID tenantId, Prescription updates) {
        Prescription prescription = getPrescriptionById(id, tenantId);

        // Only allow updates if active
        if (prescription.getStatus() == PrescriptionStatus.COMPLETED ||
            prescription.getStatus() == PrescriptionStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update completed or cancelled prescription");
        }

        if (updates.getNotes() != null) prescription.setNotes(updates.getNotes());

        return prescriptionRepository.save(prescription);
    }

    @Transactional
    public void softDeletePrescription(UUID id, UUID tenantId) {
        Prescription prescription = getPrescriptionById(id, tenantId);
        prescription.softDelete();
        prescriptionRepository.save(prescription);
        log.info("Soft deleted prescription: {}", id);
    }

    public long countPrescriptionsForPatient(UUID patientId, UUID tenantId) {
        return prescriptionRepository.countByPatientIdAndTenantId(patientId, tenantId);
    }
}
