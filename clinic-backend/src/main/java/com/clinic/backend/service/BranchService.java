package com.clinic.backend.service;

import com.clinic.common.entity.core.Branch;
import com.clinic.backend.repository.BranchRepository;
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
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchService {

    private final BranchRepository branchRepository;

    @Transactional
    @CacheEvict(value = "branches", allEntries = true, condition = "#tenantId != null")
    public Branch createBranch(Branch branch, UUID tenantId) {
        log.debug("Creating branch: {} for tenant: {}", branch.getName(), tenantId);

        // Set tenant ID
        branch.setTenantId(tenantId);

        // Uniqueness validation (Discrete Math: Set Theory)
        if (branchRepository.existsByBranchCodeAndTenantIdAndDeletedAtIsNull(
                branch.getBranchCode(), tenantId)) {
            throw new IllegalArgumentException(
                "Branch with code '" + branch.getBranchCode() + "' already exists"
            );
        }

        if (branchRepository.existsByNameAndTenantIdAndDeletedAtIsNull(
                branch.getName(), tenantId)) {
            throw new IllegalArgumentException(
                "Branch with name '" + branch.getName() + "' already exists"
            );
        }

        // If this is set as main branch, ensure no other main branch exists
        if (Boolean.TRUE.equals(branch.getIsMainBranch())) {
            Optional<Branch> existingMainBranch = branchRepository.findMainBranch(tenantId);
            if (existingMainBranch.isPresent()) {
                throw new IllegalStateException(
                    "A main branch already exists. Please unset it first or set isMainBranch to false."
                );
            }
        }

        Branch saved = branchRepository.save(branch);
        log.info("Created branch: {} with code: {}", saved.getName(), saved.getBranchCode());
        return saved;
    }

    @Cacheable(value = "branches", key = "#id + '-' + #tenantId", unless = "#result == null")
    public Branch getBranchById(UUID id, UUID tenantId) {
        return branchRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + id));
    }

    public Branch getBranchByCode(String branchCode, UUID tenantId) {
        return branchRepository.findByBranchCodeAndTenantIdAndDeletedAtIsNull(branchCode, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found with code: " + branchCode));
    }

    public List<Branch> getAllBranches(UUID tenantId) {
        return branchRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
    }

    public Page<Branch> getAllBranchesPaged(UUID tenantId, Pageable pageable) {
        return branchRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
    }

    public List<Branch> getActiveBranches(UUID tenantId) {
        return branchRepository.findByTenantIdAndIsActiveAndDeletedAtIsNull(tenantId, true);
    }

    public Optional<Branch> getMainBranch(UUID tenantId) {
        return branchRepository.findMainBranch(tenantId);
    }

    public Page<Branch> searchBranches(UUID tenantId, String search, Pageable pageable) {
        return branchRepository.searchBranches(tenantId, search, pageable);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "branches", allEntries = true, condition = "#tenantId != null"),
        @CacheEvict(value = "branches", key = "#id + '-' + #tenantId")
    })
    public Branch updateBranch(UUID id, UUID tenantId, Branch updates) {
        Branch branch = getBranchById(id, tenantId);

        // Update branch code with uniqueness check
        if (updates.getBranchCode() != null && !updates.getBranchCode().equals(branch.getBranchCode())) {
            if (branchRepository.existsByBranchCodeAndTenantIdAndDeletedAtIsNull(
                    updates.getBranchCode(), tenantId)) {
                throw new IllegalArgumentException(
                    "Branch with code '" + updates.getBranchCode() + "' already exists"
                );
            }
            branch.setBranchCode(updates.getBranchCode());
        }

        // Update name with uniqueness check
        if (updates.getName() != null && !updates.getName().equals(branch.getName())) {
            if (branchRepository.existsByNameAndTenantIdAndDeletedAtIsNull(
                    updates.getName(), tenantId)) {
                throw new IllegalArgumentException(
                    "Branch with name '" + updates.getName() + "' already exists"
                );
            }
            branch.setName(updates.getName());
        }

        // Update other fields
        if (updates.getAddress() != null) branch.setAddress(updates.getAddress());
        if (updates.getCity() != null) branch.setCity(updates.getCity());
        if (updates.getState() != null) branch.setState(updates.getState());
        if (updates.getPincode() != null) branch.setPincode(updates.getPincode());
        if (updates.getCountry() != null) branch.setCountry(updates.getCountry());
        if (updates.getPhone() != null) branch.setPhone(updates.getPhone());
        if (updates.getEmail() != null) branch.setEmail(updates.getEmail());
        if (updates.getOperatingHours() != null) branch.setOperatingHours(updates.getOperatingHours());
        if (updates.getDescription() != null) branch.setDescription(updates.getDescription());
        if (updates.getFacilities() != null) branch.setFacilities(updates.getFacilities());
        if (updates.getMaxPatientsPerDay() != null) branch.setMaxPatientsPerDay(updates.getMaxPatientsPerDay());
        if (updates.getMaxConcurrentAppointments() != null) {
            branch.setMaxConcurrentAppointments(updates.getMaxConcurrentAppointments());
        }

        // Handle main branch change
        if (updates.getIsMainBranch() != null && !updates.getIsMainBranch().equals(branch.getIsMainBranch())) {
            if (Boolean.TRUE.equals(updates.getIsMainBranch())) {
                // Setting as main branch - check if another main branch exists
                Optional<Branch> existingMainBranch = branchRepository.findMainBranch(tenantId);
                if (existingMainBranch.isPresent() && !existingMainBranch.get().getId().equals(id)) {
                    throw new IllegalStateException(
                        "Another main branch exists. Please unset it first."
                    );
                }
            }
            branch.setIsMainBranch(updates.getIsMainBranch());
        }

        if (updates.getIsActive() != null) branch.setIsActive(updates.getIsActive());

        Branch saved = branchRepository.save(branch);
        log.info("Updated branch: {}", saved.getId());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "branches", allEntries = true, condition = "#tenantId != null")
    public void activateBranch(UUID id, UUID tenantId) {
        Branch branch = getBranchById(id, tenantId);
        branch.setIsActive(true);
        branchRepository.save(branch);
        log.info("Activated branch: {}", id);
    }

    @Transactional
    @CacheEvict(value = "branches", allEntries = true, condition = "#tenantId != null")
    public void deactivateBranch(UUID id, UUID tenantId) {
        Branch branch = getBranchById(id, tenantId);

        // Cannot deactivate main branch
        if (Boolean.TRUE.equals(branch.getIsMainBranch())) {
            throw new IllegalStateException("Cannot deactivate main branch");
        }

        branch.setIsActive(false);
        branchRepository.save(branch);
        log.info("Deactivated branch: {}", id);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "branches", allEntries = true, condition = "#tenantId != null"),
        @CacheEvict(value = "branches", key = "#id + '-' + #tenantId")
    })
    public void softDeleteBranch(UUID id, UUID tenantId) {
        Branch branch = getBranchById(id, tenantId);

        // Cannot delete main branch
        if (Boolean.TRUE.equals(branch.getIsMainBranch())) {
            throw new IllegalStateException("Cannot delete main branch");
        }

        branch.softDelete();
        branchRepository.save(branch);
        log.info("Soft deleted branch: {}", id);
    }

    public long countBranches(UUID tenantId) {
        return branchRepository.countByTenantIdAndDeletedAtIsNull(tenantId);
    }

    public long countActiveBranches(UUID tenantId) {
        return branchRepository.countByTenantIdAndIsActiveAndDeletedAtIsNull(tenantId, true);
    }
}
