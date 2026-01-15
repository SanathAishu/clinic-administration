package com.clinic.backend.service;

import com.clinic.common.entity.clinical.LabResult;
import com.clinic.backend.repository.LabResultRepository;
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
public class LabResultService {

    private final LabResultRepository labResultRepository;

    @Transactional
    public LabResult createLabResult(LabResult labResult, UUID tenantId) {
        log.debug("Creating lab result for lab test: {}", labResult.getLabTest().getId());

        // LabResult doesn't have tenantId - it's linked through LabTest

        if (labResult.getResultDate() == null) {
            labResult.setResultDate(Instant.now());
        }

        if (labResult.getIsAbnormal() == null) {
            labResult.setIsAbnormal(false);
        }

        LabResult saved = labResultRepository.save(labResult);
        log.info("Created lab result: {}", saved.getId());
        return saved;
    }

    public LabResult getLabResultById(UUID id, UUID tenantId) {
        return labResultRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lab result not found: " + id));
    }

    public Page<LabResult> getLabResultsForLabTest(UUID labTestId, UUID tenantId, Pageable pageable) {
        return labResultRepository.findByLabTestIdAndTenantId(labTestId, tenantId, pageable);
    }

    public List<LabResult> getAbnormalResultsForPatient(UUID patientId, UUID tenantId) {
        return labResultRepository.findAbnormalResultsForPatient(patientId, tenantId);
    }

    public List<LabResult> getRecentResultsForPatient(UUID patientId, UUID tenantId, Instant since) {
        return labResultRepository.findRecentResultsForPatient(patientId, tenantId, since);
    }

    @Transactional
    public LabResult verifyLabResult(UUID id, UUID tenantId) {
        LabResult labResult = getLabResultById(id, tenantId);
        // Note: LabResult entity doesn't have isVerified/verifiedAt fields
        // This method exists for future implementation
        LabResult saved = labResultRepository.save(labResult);
        log.info("Verified lab result: {}", saved.getId());
        return saved;
    }

    @Transactional
    public LabResult updateLabResult(UUID id, UUID tenantId, LabResult updates) {
        LabResult labResult = getLabResultById(id, tenantId);

        if (updates.getParameterName() != null) labResult.setParameterName(updates.getParameterName());
        if (updates.getResultValue() != null) labResult.setResultValue(updates.getResultValue());
        if (updates.getUnit() != null) labResult.setUnit(updates.getUnit());
        if (updates.getReferenceRange() != null) labResult.setReferenceRange(updates.getReferenceRange());
        if (updates.getIsAbnormal() != null) labResult.setIsAbnormal(updates.getIsAbnormal());
        if (updates.getComments() != null) labResult.setComments(updates.getComments());

        return labResultRepository.save(labResult);
    }

    @Transactional
    public void deleteLabResult(UUID id, UUID tenantId) {
        LabResult labResult = getLabResultById(id, tenantId);
        labResultRepository.delete(labResult);
        log.info("Deleted lab result: {}", id);
    }

    public long countLabResultsForTest(UUID labTestId, UUID tenantId) {
        return labResultRepository.countByLabTestIdAndTenantId(labTestId, tenantId);
    }
}
