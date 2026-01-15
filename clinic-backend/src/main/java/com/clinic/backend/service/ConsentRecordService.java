package com.clinic.backend.service;

import com.clinic.common.entity.clinical.ConsentRecord;
import com.clinic.backend.repository.ConsentRecordRepository;
import com.clinic.common.enums.ConsentStatus;
import com.clinic.common.enums.ConsentType;
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
public class ConsentRecordService {

    private final ConsentRecordRepository consentRecordRepository;

    @Transactional
    public ConsentRecord createConsentRecord(ConsentRecord consentRecord, UUID tenantId) {
        log.debug("Creating consent record for patient: {}", consentRecord.getPatient().getId());

        consentRecord.setTenantId(tenantId);

        if (consentRecord.getStatus() == null) {
            consentRecord.setStatus(ConsentStatus.GRANTED);
        }

        if (consentRecord.getGrantedAt() == null) {
            consentRecord.setGrantedAt(Instant.now());
        }

        ConsentRecord saved = consentRecordRepository.save(consentRecord);
        log.info("Created consent record: {}", saved.getId());
        return saved;
    }

    public ConsentRecord getConsentRecordById(UUID id, UUID tenantId) {
        return consentRecordRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Consent record not found: " + id));
    }

    public Page<ConsentRecord> getConsentRecordsForPatient(UUID patientId, UUID tenantId, Pageable pageable) {
        return consentRecordRepository.findByPatientIdAndTenantIdAndDeletedAtIsNull(patientId, tenantId, pageable);
    }

    public List<ConsentRecord> getConsentsByType(UUID patientId, ConsentType consentType, UUID tenantId) {
        return consentRecordRepository.findByPatientIdAndConsentTypeAndTenantId(patientId, consentType, tenantId);
    }

    public List<ConsentRecord> getActiveConsentsForPatient(UUID patientId, UUID tenantId) {
        return consentRecordRepository.findActiveConsentsForPatient(patientId, tenantId);
    }

    public List<ConsentRecord> getExpiredConsents(UUID tenantId, Instant now) {
        return consentRecordRepository.findExpiredConsents(tenantId, now);
    }

    @Transactional
    public ConsentRecord revokeConsent(UUID id, UUID tenantId) {
        ConsentRecord consentRecord = getConsentRecordById(id, tenantId);

        if (consentRecord.getStatus() != ConsentStatus.GRANTED) {
            throw new IllegalStateException("Can only revoke granted consents");
        }

        consentRecord.setStatus(ConsentStatus.REVOKED);
        consentRecord.setRevokedAt(Instant.now());
        ConsentRecord saved = consentRecordRepository.save(consentRecord);
        log.info("Revoked consent record: {}", saved.getId());
        return saved;
    }

    @Transactional
    public ConsentRecord expireConsent(UUID id, UUID tenantId) {
        ConsentRecord consentRecord = getConsentRecordById(id, tenantId);
        consentRecord.setStatus(ConsentStatus.EXPIRED);
        ConsentRecord saved = consentRecordRepository.save(consentRecord);
        log.info("Expired consent record: {}", saved.getId());
        return saved;
    }

    @Transactional
    public ConsentRecord updateConsentRecord(UUID id, UUID tenantId, ConsentRecord updates) {
        ConsentRecord consentRecord = getConsentRecordById(id, tenantId);

        // Only allow updates if granted
        if (consentRecord.getStatus() != ConsentStatus.GRANTED) {
            throw new IllegalStateException("Cannot update revoked or expired consent");
        }

        if (updates.getConsentType() != null) consentRecord.setConsentType(updates.getConsentType());
        if (updates.getPurpose() != null) consentRecord.setPurpose(updates.getPurpose());
        if (updates.getExpiresAt() != null) consentRecord.setExpiresAt(updates.getExpiresAt());
        if (updates.getConsentText() != null) consentRecord.setConsentText(updates.getConsentText());

        return consentRecordRepository.save(consentRecord);
    }

    @Transactional
    public void softDeleteConsentRecord(UUID id, UUID tenantId) {
        ConsentRecord consentRecord = getConsentRecordById(id, tenantId);
        // Note: ConsentRecord doesn't extend SoftDeletableEntity, so we hard delete
        // TODO: Consider changing ConsentRecord to extend SoftDeletableEntity
        consentRecordRepository.delete(consentRecord);
        log.info("Deleted consent record: {}", id);
    }

    public long countConsentsForPatient(UUID patientId, UUID tenantId) {
        return consentRecordRepository.countByPatientIdAndTenantId(patientId, tenantId);
    }
}
