package com.clinic.backend.controller;

import com.clinic.backend.dto.treatment.CreateTreatmentRequest;
import com.clinic.backend.dto.treatment.TreatmentDTO;
import com.clinic.backend.dto.treatment.UpdateTreatmentRequest;
import com.clinic.backend.mapper.TreatmentMapper;
import com.clinic.backend.service.TreatmentService;
import com.clinic.common.entity.operational.Treatment;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/treatments")
@RequiredArgsConstructor
@PreAuthorize("@tenantValidator.isValidTenant(#tenantId)")
public class TreatmentController {

    private final TreatmentService treatmentService;
    private final TreatmentMapper treatmentMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<TreatmentDTO> createTreatment(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CreateTreatmentRequest request) {
        log.info("Creating treatment for tenant: {}", tenantId);
        Treatment treatment = treatmentService.createTreatment(
            treatmentMapper.toEntity(request), tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(treatmentMapper.toDTO(treatment));
    }

    @GetMapping("/{treatmentId}")
    public ResponseEntity<TreatmentDTO> getTreatmentById(
            @PathVariable UUID tenantId,
            @PathVariable UUID treatmentId) {
        log.debug("Getting treatment: {} for tenant: {}", treatmentId, tenantId);
        Treatment treatment = treatmentService.getTreatmentById(treatmentId, tenantId);
        return ResponseEntity.ok(treatmentMapper.toDTO(treatment));
    }

    @GetMapping
    public ResponseEntity<Page<TreatmentDTO>> getAllTreatments(
            @PathVariable UUID tenantId,
            Pageable pageable) {
        log.debug("Getting all treatments for tenant: {}", tenantId);
        Page<Treatment> treatments = treatmentService.getAllTreatments(tenantId, pageable);
        return ResponseEntity.ok(
            treatments.map(treatmentMapper::toDTO));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<TreatmentDTO>> searchTreatments(
            @PathVariable UUID tenantId,
            @RequestParam String search,
            Pageable pageable) {
        log.debug("Searching treatments for tenant: {} with query: {}", tenantId, search);
        Page<Treatment> treatments = treatmentService.searchTreatmentsByName(tenantId, search, pageable);
        return ResponseEntity.ok(
            treatments.map(treatmentMapper::toDTO));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<TreatmentDTO>> getTreatmentsByCategory(
            @PathVariable UUID tenantId,
            @PathVariable String category) {
        log.debug("Getting treatments by category: {} for tenant: {}", category, tenantId);
        List<Treatment> treatments = treatmentService.getTreatmentsByCategory(
            tenantId, category);
        return ResponseEntity.ok(treatmentMapper.toDTOList(treatments));
    }

    @PutMapping("/{treatmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<TreatmentDTO> updateTreatment(
            @PathVariable UUID tenantId,
            @PathVariable UUID treatmentId,
            @Valid @RequestBody UpdateTreatmentRequest request) {
        log.info("Updating treatment: {} for tenant: {}", treatmentId, tenantId);
        Treatment treatment = treatmentService.getTreatmentById(treatmentId, tenantId);
        treatmentMapper.updateEntityFromRequest(request, treatment);
        Treatment updated = treatmentService.updateTreatment(treatmentId, tenantId, treatment);
        return ResponseEntity.ok(treatmentMapper.toDTO(updated));
    }

    @PatchMapping("/{treatmentId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateTreatment(
            @PathVariable UUID tenantId,
            @PathVariable UUID treatmentId) {
        log.info("Activating treatment: {} for tenant: {}", treatmentId, tenantId);
        treatmentService.activateTreatment(treatmentId, tenantId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{treatmentId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateTreatment(
            @PathVariable UUID tenantId,
            @PathVariable UUID treatmentId) {
        log.info("Deactivating treatment: {} for tenant: {}", treatmentId, tenantId);
        treatmentService.deactivateTreatment(treatmentId, tenantId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{treatmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTreatment(
            @PathVariable UUID tenantId,
            @PathVariable UUID treatmentId) {
        log.info("Deleting treatment: {} for tenant: {}", treatmentId, tenantId);
        treatmentService.softDeleteTreatment(treatmentId, tenantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countTreatments(
            @PathVariable UUID tenantId) {
        log.debug("Counting treatments for tenant: {}", tenantId);
        long count = treatmentService.countTreatments(tenantId);
        return ResponseEntity.ok(count);
    }
}
