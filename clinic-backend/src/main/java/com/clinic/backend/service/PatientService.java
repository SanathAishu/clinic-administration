package com.clinic.backend.service;

import com.clinic.common.entity.patient.Patient;
import com.clinic.common.entity.core.Tenant;
import com.clinic.backend.repository.PatientRepository;
import com.clinic.common.enums.Gender;
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
public class PatientService {

    private final PatientRepository patientRepository;
    private final TenantService tenantService;

    @Transactional
    public Patient createPatient(Patient patient, UUID tenantId) {
        log.debug("Creating patient: {} for tenant: {}", patient.getEmail(), tenantId);

        // Validate tenant capacity
        Tenant tenant = tenantService.getTenantById(tenantId);
        long currentCount = patientRepository.countByTenantIdAndDeletedAtIsNull(tenantId);
        if (currentCount >= tenant.getMaxPatients()) {
            throw new IllegalStateException(
                    String.format("Tenant patient limit reached: %d/%d", currentCount, tenant.getMaxPatients()));
        }

        // Uniqueness validation
        if (patient.getEmail() != null && patientRepository.existsByEmailAndTenantId(patient.getEmail(), tenantId)) {
            throw new IllegalArgumentException("Email already exists: " + patient.getEmail());
        }

        if (patient.getPhone() != null && patientRepository.existsByPhoneAndTenantId(patient.getPhone(), tenantId)) {
            throw new IllegalArgumentException("Phone already exists: " + patient.getPhone());
        }

        if (patient.getAbhaId() != null && patientRepository.existsByAbhaIdAndTenantId(patient.getAbhaId(), tenantId)) {
            throw new IllegalArgumentException("ABHA ID already exists: " + patient.getAbhaId());
        }

        patient.setTenantId(tenantId);
        Patient saved = patientRepository.save(patient);
        log.info("Created patient: {}", saved.getId());
        return saved;
    }

    public Patient getPatientById(UUID id, UUID tenantId) {
        return patientRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + id));
    }

    public Page<Patient> getAllPatientsForTenant(UUID tenantId, Pageable pageable) {
        return patientRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
    }

    public Page<Patient> searchPatients(UUID tenantId, String search, Pageable pageable) {
        return patientRepository.searchPatients(tenantId, search, pageable);
    }

    public List<Patient> getPatientsByGender(UUID tenantId, Gender gender) {
        return patientRepository.findByTenantIdAndGenderAndDeletedAtIsNull(tenantId, gender);
    }

    public List<Patient> getPatientsWithAbha(UUID tenantId) {
        return patientRepository.findPatientsWithAbha(tenantId);
    }

    @Transactional
    public Patient updatePatient(UUID id, UUID tenantId, Patient updates) {
        Patient patient = getPatientById(id, tenantId);

        if (updates.getFirstName() != null) patient.setFirstName(updates.getFirstName());
        if (updates.getLastName() != null) patient.setLastName(updates.getLastName());
        if (updates.getDateOfBirth() != null) patient.setDateOfBirth(updates.getDateOfBirth());
        if (updates.getGender() != null) patient.setGender(updates.getGender());
        if (updates.getBloodGroup() != null) patient.setBloodGroup(updates.getBloodGroup());
        if (updates.getAddressLine1() != null) patient.setAddressLine1(updates.getAddressLine1());
        if (updates.getAddressLine2() != null) patient.setAddressLine2(updates.getAddressLine2());

        if (updates.getEmail() != null && !updates.getEmail().equals(patient.getEmail())) {
            if (patientRepository.existsByEmailAndTenantId(updates.getEmail(), tenantId)) {
                throw new IllegalArgumentException("Email already exists: " + updates.getEmail());
            }
            patient.setEmail(updates.getEmail());
        }

        return patientRepository.save(patient);
    }

    @Transactional
    public void softDeletePatient(UUID id, UUID tenantId) {
        Patient patient = getPatientById(id, tenantId);
        patient.softDelete();
        patientRepository.save(patient);
        log.info("Soft deleted patient: {}", id);
    }

    public long countPatientsForTenant(UUID tenantId) {
        return patientRepository.countByTenantIdAndDeletedAtIsNull(tenantId);
    }
}
