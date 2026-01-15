package com.clinic.backend.service;

import com.clinic.backend.mapper.PatientViewMapper;
import com.clinic.backend.repository.PatientViewRepository;
import com.clinic.common.dto.view.*;
import com.clinic.common.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for read-only patient operations using database views (CQRS pattern).
 *
 * This service handles all READ operations for patients by querying
 * database views directly. This provides:
 * - Pre-computed fields (no application-level calculation)
 * - Pre-joined data (no N+1 queries)
 * - Database-optimized queries
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientViewService {

    private final PatientViewRepository patientViewRepository;
    private final PatientViewMapper patientViewMapper;

    /**
     * Get paginated list of active patients for the current tenant.
     *
     * @param pageable pagination parameters
     * @return page of PatientListViewDTO
     */
    public Page<PatientListViewDTO> getPatientList(Pageable pageable) {
        UUID tenantId = getCurrentTenantId();
        log.debug("Fetching patient list for tenant: {}", tenantId);

        Page<Object[]> results = patientViewRepository.findAllPatientsForTenant(tenantId, pageable);

        List<PatientListViewDTO> dtos = results.getContent().stream()
            .map(patientViewMapper::toPatientListViewDTO)
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    /**
     * Search patients by name, phone, email, or ABHA ID.
     *
     * @param searchTerm the search term
     * @param pageable pagination parameters
     * @return page of matching PatientListViewDTO
     */
    public Page<PatientListViewDTO> searchPatients(String searchTerm, Pageable pageable) {
        UUID tenantId = getCurrentTenantId();
        log.debug("Searching patients for tenant: {} with term: {}", tenantId, searchTerm);

        Page<Object[]> results = patientViewRepository.searchPatients(tenantId, searchTerm, pageable);

        List<PatientListViewDTO> dtos = results.getContent().stream()
            .map(patientViewMapper::toPatientListViewDTO)
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    /**
     * Get detailed patient information including latest vitals and counts.
     *
     * @param patientId the patient ID
     * @return Optional of PatientDetailViewDTO
     */
    public Optional<PatientDetailViewDTO> getPatientDetail(UUID patientId) {
        UUID tenantId = getCurrentTenantId();
        log.debug("Fetching patient detail for patient: {} in tenant: {}", patientId, tenantId);

        Object[] result = patientViewRepository.findPatientDetailById(patientId, tenantId);

        if (result == null || result.length == 0 || result[0] == null) {
            return Optional.empty();
        }

        return Optional.of(patientViewMapper.toPatientDetailViewDTO(result));
    }

    /**
     * Get patient's appointment history.
     *
     * @param patientId the patient ID
     * @param pageable pagination parameters
     * @return page of PatientAppointmentViewDTO
     */
    public Page<PatientAppointmentViewDTO> getPatientAppointments(UUID patientId, Pageable pageable) {
        UUID tenantId = getCurrentTenantId();
        log.debug("Fetching appointments for patient: {} in tenant: {}", patientId, tenantId);

        Page<Object[]> results = patientViewRepository.findPatientAppointments(patientId, tenantId, pageable);

        List<PatientAppointmentViewDTO> dtos = results.getContent().stream()
            .map(patientViewMapper::toPatientAppointmentViewDTO)
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    /**
     * Get patient's medical record history.
     *
     * @param patientId the patient ID
     * @param pageable pagination parameters
     * @return page of PatientMedicalHistoryViewDTO
     */
    public Page<PatientMedicalHistoryViewDTO> getPatientMedicalHistory(UUID patientId, Pageable pageable) {
        UUID tenantId = getCurrentTenantId();
        log.debug("Fetching medical history for patient: {} in tenant: {}", patientId, tenantId);

        Page<Object[]> results = patientViewRepository.findPatientMedicalHistory(patientId, tenantId, pageable);

        List<PatientMedicalHistoryViewDTO> dtos = results.getContent().stream()
            .map(patientViewMapper::toPatientMedicalHistoryViewDTO)
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    /**
     * Get patient's billing history.
     *
     * @param patientId the patient ID
     * @param pageable pagination parameters
     * @return page of PatientBillingHistoryViewDTO
     */
    public Page<PatientBillingHistoryViewDTO> getPatientBillingHistory(UUID patientId, Pageable pageable) {
        UUID tenantId = getCurrentTenantId();
        log.debug("Fetching billing history for patient: {} in tenant: {}", patientId, tenantId);

        Page<Object[]> results = patientViewRepository.findPatientBillingHistory(patientId, tenantId, pageable);

        List<PatientBillingHistoryViewDTO> dtos = results.getContent().stream()
            .map(patientViewMapper::toPatientBillingHistoryViewDTO)
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    /**
     * Get the current tenant ID from TenantContext.
     * Throws IllegalStateException if tenant context is not set.
     *
     * @return the current tenant ID
     */
    private UUID getCurrentTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not set. Ensure authentication is complete.");
        }
        return tenantId;
    }
}
