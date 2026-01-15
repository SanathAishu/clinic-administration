package com.clinic.backend.service;

import com.clinic.common.entity.patient.Vital;
import com.clinic.backend.repository.VitalRepository;
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
public class VitalService {

    private final VitalRepository vitalRepository;

    @Transactional
    public Vital createVital(Vital vital, UUID tenantId) {
        log.debug("Creating vital for patient: {}", vital.getPatient().getId());

        vital.setTenantId(tenantId);

        if (vital.getRecordedAt() == null) {
            vital.setRecordedAt(Instant.now());
        }

        Vital saved = vitalRepository.save(vital);
        log.info("Created vital: {}", saved.getId());
        return saved;
    }

    public Vital getVitalById(UUID id, UUID tenantId) {
        return vitalRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Vital not found: " + id));
    }

    public Page<Vital> getVitalsForPatient(UUID patientId, UUID tenantId, Pageable pageable) {
        return vitalRepository.findByPatientIdAndTenantId(patientId, tenantId, pageable);
    }

    public List<Vital> getVitalsForAppointment(UUID appointmentId, UUID tenantId) {
        return vitalRepository.findByAppointmentIdAndTenantId(appointmentId, tenantId);
    }

    public List<Vital> getRecentVitalsForPatient(UUID patientId, UUID tenantId, Instant since) {
        return vitalRepository.findRecentVitalsForPatient(patientId, tenantId, since);
    }

    @Transactional
    public Vital updateVital(UUID id, UUID tenantId, Vital updates) {
        Vital vital = getVitalById(id, tenantId);

        if (updates.getTemperatureCelsius() != null) vital.setTemperatureCelsius(updates.getTemperatureCelsius());
        if (updates.getSystolicBp() != null) vital.setSystolicBp(updates.getSystolicBp());
        if (updates.getDiastolicBp() != null) vital.setDiastolicBp(updates.getDiastolicBp());
        if (updates.getPulseBpm() != null) vital.setPulseBpm(updates.getPulseBpm());
        if (updates.getRespiratoryRate() != null) vital.setRespiratoryRate(updates.getRespiratoryRate());
        if (updates.getOxygenSaturation() != null) vital.setOxygenSaturation(updates.getOxygenSaturation());
        if (updates.getWeightKg() != null) vital.setWeightKg(updates.getWeightKg());
        if (updates.getHeightCm() != null) vital.setHeightCm(updates.getHeightCm());
        if (updates.getBmi() != null) vital.setBmi(updates.getBmi());
        if (updates.getBloodGlucoseMgdl() != null) vital.setBloodGlucoseMgdl(updates.getBloodGlucoseMgdl());
        if (updates.getNotes() != null) vital.setNotes(updates.getNotes());

        return vitalRepository.save(vital);
    }

    @Transactional
    public void deleteVital(UUID id, UUID tenantId) {
        Vital vital = getVitalById(id, tenantId);
        vitalRepository.delete(vital);
        log.info("Deleted vital: {}", id);
    }

    public long countVitalsForPatient(UUID patientId, UUID tenantId) {
        return vitalRepository.countByPatientIdAndTenantId(patientId, tenantId);
    }
}
