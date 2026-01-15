package com.clinic.backend.service;

import com.clinic.common.entity.patient.PatientDocument;
import com.clinic.backend.repository.PatientDocumentRepository;
import com.clinic.common.enums.DocumentType;
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
public class PatientDocumentService {

    private final PatientDocumentRepository patientDocumentRepository;

    @Transactional
    public PatientDocument createDocument(PatientDocument document, UUID tenantId) {
        log.debug("Creating document for patient: {}", document.getPatient().getId());

        document.setTenantId(tenantId);

        if (document.getUploadedAt() == null) {
            document.setUploadedAt(Instant.now());
        }

        PatientDocument saved = patientDocumentRepository.save(document);
        log.info("Created patient document: {}", saved.getId());
        return saved;
    }

    public PatientDocument getDocumentById(UUID id, UUID tenantId) {
        return patientDocumentRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
    }

    public Page<PatientDocument> getDocumentsForPatient(UUID patientId, UUID tenantId, Pageable pageable) {
        return patientDocumentRepository.findByPatientIdAndTenantIdAndDeletedAtIsNull(patientId, tenantId, pageable);
    }

    public List<PatientDocument> getDocumentsByType(UUID patientId, DocumentType documentType, UUID tenantId) {
        return patientDocumentRepository.findByPatientIdAndDocumentTypeAndTenantId(patientId, documentType, tenantId);
    }

    public List<PatientDocument> getRecentDocumentsForPatient(UUID patientId, UUID tenantId, Instant since) {
        return patientDocumentRepository.findRecentDocumentsForPatient(patientId, tenantId, since);
    }

    @Transactional
    public PatientDocument updateDocument(UUID id, UUID tenantId, PatientDocument updates) {
        PatientDocument document = getDocumentById(id, tenantId);

        if (updates.getTitle() != null) document.setTitle(updates.getTitle());
        if (updates.getDocumentType() != null) document.setDocumentType(updates.getDocumentType());
        if (updates.getDescription() != null) document.setDescription(updates.getDescription());

        return patientDocumentRepository.save(document);
    }

    @Transactional
    public void softDeleteDocument(UUID id, UUID tenantId) {
        PatientDocument document = getDocumentById(id, tenantId);
        document.softDelete();
        patientDocumentRepository.save(document);
        log.info("Soft deleted patient document: {}", id);
    }

    public long countDocumentsForPatient(UUID patientId, UUID tenantId) {
        return patientDocumentRepository.countByPatientIdAndTenantId(patientId, tenantId);
    }
}
