package com.clinic.backend.controller;

import com.clinic.backend.dto.branch.CreateBranchRequest;
import com.clinic.backend.dto.branch.BranchDTO;
import com.clinic.backend.dto.branch.UpdateBranchRequest;
import com.clinic.backend.mapper.BranchMapper;
import com.clinic.backend.service.BranchService;
import com.clinic.common.entity.core.Branch;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/branches")
@RequiredArgsConstructor
@PreAuthorize("@tenantValidator.isValidTenant(#tenantId)")
public class BranchController {

    private final BranchService branchService;
    private final BranchMapper branchMapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BranchDTO> createBranch(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CreateBranchRequest request) {
        log.info("Creating branch for tenant: {}", tenantId);
        Branch branch = branchService.createBranch(
            branchMapper.toEntity(request), tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(branchMapper.toDTO(branch));
    }

    @GetMapping("/{branchId}")
    public ResponseEntity<BranchDTO> getBranchById(
            @PathVariable UUID tenantId,
            @PathVariable UUID branchId) {
        log.debug("Getting branch: {} for tenant: {}", branchId, tenantId);
        Branch branch = branchService.getBranchById(branchId, tenantId);
        return ResponseEntity.ok(branchMapper.toDTO(branch));
    }

    @GetMapping("/code/{branchCode}")
    public ResponseEntity<BranchDTO> getBranchByCode(
            @PathVariable UUID tenantId,
            @PathVariable String branchCode) {
        log.debug("Getting branch by code: {} for tenant: {}", branchCode, tenantId);
        Branch branch = branchService.getBranchByCode(branchCode, tenantId);
        return ResponseEntity.ok(branchMapper.toDTO(branch));
    }

    @GetMapping
    public ResponseEntity<Page<BranchDTO>> getAllBranches(
            @PathVariable UUID tenantId,
            Pageable pageable) {
        log.debug("Getting all branches for tenant: {}", tenantId);
        Page<Branch> branches = branchService.getAllBranchesPaged(tenantId, pageable);
        return ResponseEntity.ok(
            branches.map(branchMapper::toDTO));
    }

    @GetMapping("/active")
    public ResponseEntity<Page<BranchDTO>> getActiveBranches(
            @PathVariable UUID tenantId,
            Pageable pageable) {
        log.debug("Getting active branches for tenant: {}", tenantId);
        var activeBranches = branchService.getActiveBranches(tenantId);
        return ResponseEntity.ok(
            new org.springframework.data.domain.PageImpl<>(
                branchMapper.toDTOList(activeBranches), pageable, activeBranches.size()
            ));
    }

    @GetMapping("/main")
    public ResponseEntity<BranchDTO> getMainBranch(
            @PathVariable UUID tenantId) {
        log.debug("Getting main branch for tenant: {}", tenantId);
        return branchService.getMainBranch(tenantId)
            .map(branch -> ResponseEntity.ok(branchMapper.toDTO(branch)))
            .orElseThrow(() -> new IllegalArgumentException("Main branch not found for tenant: " + tenantId));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<BranchDTO>> searchBranches(
            @PathVariable UUID tenantId,
            @RequestParam String search,
            Pageable pageable) {
        log.debug("Searching branches for tenant: {} with query: {}", tenantId, search);
        Page<Branch> branches = branchService.searchBranches(tenantId, search, pageable);
        return ResponseEntity.ok(
            branches.map(branchMapper::toDTO));
    }

    @PutMapping("/{branchId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BranchDTO> updateBranch(
            @PathVariable UUID tenantId,
            @PathVariable UUID branchId,
            @Valid @RequestBody UpdateBranchRequest request) {
        log.info("Updating branch: {} for tenant: {}", branchId, tenantId);
        Branch branch = branchService.getBranchById(branchId, tenantId);
        branchMapper.updateEntityFromRequest(request, branch);
        Branch updated = branchService.updateBranch(branchId, tenantId, branch);
        return ResponseEntity.ok(branchMapper.toDTO(updated));
    }

    @PatchMapping("/{branchId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateBranch(
            @PathVariable UUID tenantId,
            @PathVariable UUID branchId) {
        log.info("Activating branch: {} for tenant: {}", branchId, tenantId);
        branchService.activateBranch(branchId, tenantId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{branchId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateBranch(
            @PathVariable UUID tenantId,
            @PathVariable UUID branchId) {
        log.info("Deactivating branch: {} for tenant: {}", branchId, tenantId);
        branchService.deactivateBranch(branchId, tenantId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{branchId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBranch(
            @PathVariable UUID tenantId,
            @PathVariable UUID branchId) {
        log.info("Deleting branch: {} for tenant: {}", branchId, tenantId);
        branchService.softDeleteBranch(branchId, tenantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countBranches(
            @PathVariable UUID tenantId) {
        log.debug("Counting branches for tenant: {}", tenantId);
        long count = branchService.countBranches(tenantId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/active")
    public ResponseEntity<Long> countActiveBranches(
            @PathVariable UUID tenantId) {
        log.debug("Counting active branches for tenant: {}", tenantId);
        long count = branchService.countActiveBranches(tenantId);
        return ResponseEntity.ok(count);
    }
}
