package com.clinic.backend.service;

import com.clinic.common.entity.operational.Treatment;
import com.clinic.backend.repository.TreatmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TreatmentService {

    private final TreatmentRepository treatmentRepository;

    @Transactional
    @CacheEvict(value = "treatments", allEntries = true, condition = "#tenantId != null")
    public Treatment createTreatment(Treatment treatment, UUID tenantId) {
        log.debug("Creating treatment: {} for tenant: {}", treatment.getName(), tenantId);

        // Set tenant ID
        treatment.setTenantId(tenantId);

        // Uniqueness validation (Discrete Math: Set Theory - uniqueness constraint)
        if (treatmentRepository.existsByNameAndTenantIdAndDeletedAtIsNull(
                treatment.getName(), tenantId)) {
            throw new IllegalArgumentException(
                "Treatment with name '" + treatment.getName() + "' already exists"
            );
        }

        Treatment saved = treatmentRepository.save(treatment);
        log.info("Created treatment: {} with ID: {}", saved.getName(), saved.getId());
        return saved;
    }

    @Cacheable(value = "treatments", key = "#id + '-' + #tenantId", unless = "#result == null")
    public Treatment getTreatmentById(UUID id, UUID tenantId) {
        return treatmentRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + id));
    }

    public Page<Treatment> getAllTreatments(UUID tenantId, Pageable pageable) {
        return treatmentRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
    }

    public Page<Treatment> searchTreatmentsByName(UUID tenantId, String name, Pageable pageable) {
        return treatmentRepository.searchByName(tenantId, name, pageable);
    }

    public List<Treatment> getTreatmentsByCategory(UUID tenantId, String category) {
        return treatmentRepository.findByTenantIdAndCategoryAndDeletedAtIsNull(tenantId, category);
    }

    public List<Treatment> getActiveTreatments(UUID tenantId) {
        return treatmentRepository.findByTenantIdAndIsActiveAndDeletedAtIsNull(tenantId, true);
    }

    public List<Treatment> getActiveTreatmentsByCategory(UUID tenantId, String category) {
        return treatmentRepository.findActiveTreatmentsByCategory(tenantId, category);
    }

    public List<String> getAllCategories(UUID tenantId) {
        return treatmentRepository.findAllCategories(tenantId);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "treatments", allEntries = true, condition = "#tenantId != null"),
        @CacheEvict(value = "treatments", key = "#id + '-' + #tenantId")
    })
    public Treatment updateTreatment(UUID id, UUID tenantId, Treatment updates) {
        Treatment treatment = getTreatmentById(id, tenantId);

        // Update name with uniqueness check
        if (updates.getName() != null && !updates.getName().equals(treatment.getName())) {
            if (treatmentRepository.existsByNameAndTenantIdAndDeletedAtIsNull(
                    updates.getName(), tenantId)) {
                throw new IllegalArgumentException(
                    "Treatment with name '" + updates.getName() + "' already exists"
                );
            }
            treatment.setName(updates.getName());
        }

        // Update other fields
        if (updates.getDescription() != null) {
            treatment.setDescription(updates.getDescription());
        }

        if (updates.getCategory() != null) {
            treatment.setCategory(updates.getCategory());
        }

        if (updates.getBaseCost() != null) {
            treatment.setBaseCost(updates.getBaseCost());
        }

        if (updates.getDiscountPercentage() != null) {
            treatment.setDiscountPercentage(updates.getDiscountPercentage());
        }

        if (updates.getDurationMinutes() != null) {
            treatment.setDurationMinutes(updates.getDurationMinutes());
        }

        if (updates.getInstructions() != null) {
            treatment.setInstructions(updates.getInstructions());
        }

        if (updates.getPrerequisites() != null) {
            treatment.setPrerequisites(updates.getPrerequisites());
        }

        if (updates.getIsActive() != null) {
            treatment.setIsActive(updates.getIsActive());
        }

        Treatment saved = treatmentRepository.save(treatment);
        log.info("Updated treatment: {}", saved.getId());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "treatments", allEntries = true, condition = "#tenantId != null")
    public void activateTreatment(UUID id, UUID tenantId) {
        Treatment treatment = getTreatmentById(id, tenantId);
        treatment.setIsActive(true);
        treatmentRepository.save(treatment);
        log.info("Activated treatment: {}", id);
    }

    @Transactional
    @CacheEvict(value = "treatments", allEntries = true, condition = "#tenantId != null")
    public void deactivateTreatment(UUID id, UUID tenantId) {
        Treatment treatment = getTreatmentById(id, tenantId);
        treatment.setIsActive(false);
        treatmentRepository.save(treatment);
        log.info("Deactivated treatment: {}", id);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "treatments", allEntries = true, condition = "#tenantId != null"),
        @CacheEvict(value = "treatments", key = "#id + '-' + #tenantId")
    })
    public void softDeleteTreatment(UUID id, UUID tenantId) {
        Treatment treatment = getTreatmentById(id, tenantId);
        treatment.softDelete();
        treatmentRepository.save(treatment);
        log.info("Soft deleted treatment: {}", id);
    }

    public long countTreatments(UUID tenantId) {
        return treatmentRepository.countByTenantIdAndDeletedAtIsNull(tenantId);
    }

    public long countActiveTreatments(UUID tenantId) {
        return treatmentRepository.countByTenantIdAndIsActiveAndDeletedAtIsNull(tenantId, true);
    }
}
