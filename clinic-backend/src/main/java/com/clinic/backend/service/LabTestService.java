package com.clinic.backend.service;

import com.clinic.common.entity.clinical.LabTest;
import com.clinic.backend.repository.LabTestRepository;
import com.clinic.common.enums.LabTestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabTestService {

    private final LabTestRepository labTestRepository;

    @Transactional
    public LabTest createLabTest(LabTest labTest, UUID tenantId) {
        log.debug("Creating lab test for patient: {}", labTest.getPatient().getId());

        labTest.setTenantId(tenantId);

        if (labTest.getStatus() == null) {
            labTest.setStatus(LabTestStatus.ORDERED);
        }

        if (labTest.getOrderedAt() == null) {
            labTest.setOrderedAt(Instant.now());
        }

        LabTest saved = labTestRepository.save(labTest);
        log.info("Created lab test: {}", saved.getId());
        return saved;
    }

    public LabTest getLabTestById(UUID id, UUID tenantId) {
        return labTestRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Lab test not found: " + id));
    }

    public Page<LabTest> getLabTestsForPatient(UUID patientId, UUID tenantId, Pageable pageable) {
        return labTestRepository.findByPatientIdAndTenantIdAndDeletedAtIsNull(patientId, tenantId, pageable);
    }

    public List<LabTest> getLabTestsByDoctor(UUID doctorId, UUID tenantId) {
        return labTestRepository.findByOrderedByIdAndTenantId(doctorId, tenantId);
    }

    public List<LabTest> getLabTestsByStatus(UUID tenantId, LabTestStatus status) {
        return labTestRepository.findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status);
    }

    public List<LabTest> getPendingLabTests(UUID tenantId) {
        return labTestRepository.findPendingLabTests(tenantId);
    }

    @Transactional
    public LabTest collectSample(UUID id, UUID tenantId) {
        LabTest labTest = getLabTestById(id, tenantId);

        if (labTest.getStatus() != LabTestStatus.ORDERED) {
            throw new IllegalStateException("Can only collect sample for ordered tests");
        }

        labTest.setStatus(LabTestStatus.SAMPLE_COLLECTED);
        labTest.setSampleCollectedAt(Instant.now());
        LabTest saved = labTestRepository.save(labTest);
        log.info("Sample collected for lab test: {}", saved.getId());
        return saved;
    }

    @Transactional
    public LabTest markInProgress(UUID id, UUID tenantId) {
        LabTest labTest = getLabTestById(id, tenantId);
        labTest.setStatus(LabTestStatus.IN_PROGRESS);
        LabTest saved = labTestRepository.save(labTest);
        log.info("Lab test in progress: {}", saved.getId());
        return saved;
    }

    @Transactional
    public LabTest completeLabTest(UUID id, UUID tenantId) {
        LabTest labTest = getLabTestById(id, tenantId);
        labTest.setStatus(LabTestStatus.COMPLETED);
        labTest.setCompletedAt(Instant.now());
        LabTest saved = labTestRepository.save(labTest);
        log.info("Completed lab test: {}", saved.getId());
        return saved;
    }

    @Transactional
    public LabTest cancelLabTest(UUID id, UUID tenantId) {
        LabTest labTest = getLabTestById(id, tenantId);
        labTest.setStatus(LabTestStatus.CANCELLED);
        LabTest saved = labTestRepository.save(labTest);
        log.info("Cancelled lab test: {}", saved.getId());
        return saved;
    }

    @Transactional
    public LabTest updateLabTest(UUID id, UUID tenantId, LabTest updates) {
        LabTest labTest = getLabTestById(id, tenantId);

        if (updates.getTestName() != null) labTest.setTestName(updates.getTestName());
        if (updates.getTestCode() != null) labTest.setTestCode(updates.getTestCode());
        if (updates.getInstructions() != null) labTest.setInstructions(updates.getInstructions());

        return labTestRepository.save(labTest);
    }

    @Transactional
    public void softDeleteLabTest(UUID id, UUID tenantId) {
        LabTest labTest = getLabTestById(id, tenantId);
        labTest.softDelete();
        labTestRepository.save(labTest);
        log.info("Soft deleted lab test: {}", id);
    }

    public long countLabTestsForPatient(UUID patientId, UUID tenantId) {
        return labTestRepository.countByPatientIdAndTenantId(patientId, tenantId);
    }
}
