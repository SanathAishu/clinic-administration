package com.clinic.backend.controller;

import com.clinic.common.entity.compliance.DataRetentionPolicy;
import com.clinic.common.entity.compliance.DataArchivalLog;
import com.clinic.common.enums.EntityType;
import com.clinic.backend.dto.compliance.DataRetentionPolicyDTO;
import com.clinic.backend.dto.compliance.DataArchivalLogDTO;
import com.clinic.backend.service.DataRetentionService;
import com.clinic.backend.repository.DataRetentionPolicyRepository;
import com.clinic.backend.repository.DataArchivalLogRepository;
import com.clinic.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Data Retention Policy Management and Archival Tracking
 *
 * Provides endpoints for managing data retention policies and reviewing
 * archival execution logs. ISO 27001 A.18.1.3 aligned.
 *
 * Security: Requires ADMIN or COMPLIANCE_OFFICER role
 */
@RestController
@RequestMapping("/api/retention")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN', 'COMPLIANCE_OFFICER')")
@Slf4j
public class DataRetentionController {

    private final DataRetentionService dataRetentionService;
    private final DataRetentionPolicyRepository policyRepository;
    private final DataArchivalLogRepository archivalLogRepository;
    private final SecurityUtils securityUtils;

    /**
     * GET /api/retention/policies
     * Get all retention policies for current tenant
     *
     * @return List of retention policies
     */
    @GetMapping("/policies")
    public ResponseEntity<List<DataRetentionPolicyDTO>> getPolicies() {
        UUID tenantId = securityUtils.getCurrentTenantId();
        log.info("Fetching retention policies for tenant {}", tenantId);

        try {
            // Placeholder: would query policies in full implementation
            List<DataRetentionPolicy> policies = List.of();

            List<DataRetentionPolicyDTO> dtos = policies.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Error fetching retention policies: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/retention/policies/{entityType}
     * Get retention policy for specific entity type
     *
     * @param entityType Entity type (AUDIT_LOG, APPOINTMENT, etc.)
     * @return Retention policy DTO
     */
    @GetMapping("/policies/{entityType}")
    public ResponseEntity<DataRetentionPolicyDTO> getPolicy(@PathVariable String entityType) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        log.info("Fetching retention policy for {} in tenant {}", entityType, tenantId);

        try {
            EntityType type = EntityType.valueOf(entityType);
            DataRetentionPolicy policy = dataRetentionService.getPolicyForEntityType(type, tenantId);

            if (policy == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(toDTO(policy));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid entity type: {}", entityType);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching retention policy: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/retention/policies/{entityType}
     * Update retention policy
     *
     * @param entityType Entity type
     * @param policyDTO Updated policy
     * @return Updated policy DTO
     */
    @PutMapping("/policies/{entityType}")
    public ResponseEntity<DataRetentionPolicyDTO> updatePolicy(
            @PathVariable String entityType,
            @RequestBody DataRetentionPolicyDTO policyDTO) {

        UUID tenantId = securityUtils.getCurrentTenantId();
        log.info("Updating retention policy for {} in tenant {}", entityType, tenantId);

        try {
            EntityType type = EntityType.valueOf(entityType);
            DataRetentionPolicy existingPolicy = dataRetentionService.getPolicyForEntityType(type, tenantId);

            if (existingPolicy == null) {
                return ResponseEntity.notFound().build();
            }

            existingPolicy.setRetentionDays(policyDTO.getRetentionDays());
            existingPolicy.setGracePeriodDays(policyDTO.getGracePeriodDays());
            existingPolicy.setArchivalAction(policyDTO.getArchivalAction());
            existingPolicy.setEnabled(policyDTO.getEnabled());

            DataRetentionPolicy updated = policyRepository.save(existingPolicy);
            log.info("Updated retention policy for {}", entityType);

            return ResponseEntity.ok(toDTO(updated));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid entity type: {}", entityType);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating retention policy: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/retention/archival-logs?startDate=2025-01-01&endDate=2025-01-31
     * Get archival execution logs for date range
     *
     * @param startDate Range start
     * @param endDate Range end
     * @return List of archival logs
     */
    @GetMapping("/archival-logs")
    public ResponseEntity<List<DataArchivalLogDTO>> getArchivalLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        UUID tenantId = securityUtils.getCurrentTenantId();
        LocalDate actualStart = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate actualEnd = endDate != null ? endDate : LocalDate.now();

        log.info("Fetching archival logs from {} to {}", actualStart, actualEnd);

        try {
            List<DataArchivalLog> logs = dataRetentionService.getArchivalLogs(actualStart, actualEnd, tenantId);

            List<DataArchivalLogDTO> dtos = logs.stream()
                .map(this::toArchivalLogDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Error fetching archival logs: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/retention/archival-logs/recent?days=7
     * Get recent successful archival executions
     *
     * @param days Number of days to look back
     * @return List of successful archival logs
     */
    @GetMapping("/archival-logs/recent")
    public ResponseEntity<List<DataArchivalLogDTO>> getRecentArchivalLogs(
            @RequestParam(defaultValue = "7") int days) {

        UUID tenantId = securityUtils.getCurrentTenantId();
        log.info("Fetching recent archival logs for last {} days", days);

        try {
            List<DataArchivalLog> logs = dataRetentionService.getSuccessfulArchivalLogs(days, tenantId);

            List<DataArchivalLogDTO> dtos = logs.stream()
                .map(this::toArchivalLogDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Error fetching recent archival logs: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/retention/archival-logs/failed
     * Get failed archival executions requiring attention
     *
     * @return List of failed archival logs
     */
    @GetMapping("/archival-logs/failed")
    public ResponseEntity<List<DataArchivalLogDTO>> getFailedArchivalLogs() {
        UUID tenantId = securityUtils.getCurrentTenantId();
        log.info("Fetching failed archival logs for tenant {}", tenantId);

        try {
            List<DataArchivalLog> logs = dataRetentionService.getFailedArchivalLogs(tenantId);

            List<DataArchivalLogDTO> dtos = logs.stream()
                .map(this::toArchivalLogDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Error fetching failed archival logs: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/retention/execute/{entityType}
     * Manually trigger retention policy execution
     *
     * @param entityType Entity type to archive
     * @return Execution status message
     */
    @PostMapping("/execute/{entityType}")
    public ResponseEntity<String> executePolicy(@PathVariable String entityType) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        log.info("Manually triggering retention policy for {} in tenant {}", entityType, tenantId);

        try {
            EntityType type = EntityType.valueOf(entityType);
            DataRetentionPolicy policy = dataRetentionService.getPolicyForEntityType(type, tenantId);

            if (policy == null) {
                return ResponseEntity.notFound().build();
            }

            dataRetentionService.executeRetentionPolicies();
            return ResponseEntity.ok("Retention policy execution initiated for " + entityType);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid entity type: {}", entityType);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error executing retention policy: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Helper: Convert DataRetentionPolicy to DTO
     */
    private DataRetentionPolicyDTO toDTO(DataRetentionPolicy policy) {
        return DataRetentionPolicyDTO.builder()
            .id(policy.getId())
            .entityType(policy.getEntityType())
            .retentionDays(policy.getRetentionDays())
            .gracePeriodDays(policy.getGracePeriodDays())
            .archivalAction(policy.getArchivalAction())
            .enabled(policy.getEnabled())
            .lastExecution(policy.getLastExecution())
            .recordsArchived(policy.getRecordsArchived())
            .createdAt(policy.getCreatedAt())
            .updatedAt(policy.getUpdatedAt())
            .build();
    }

    /**
     * Helper: Convert DataArchivalLog to DTO
     */
    private DataArchivalLogDTO toArchivalLogDTO(DataArchivalLog log) {
        String entityTypeStr = log.getEntityType() != null ? log.getEntityType().toString() : "UNKNOWN";

        return DataArchivalLogDTO.builder()
            .id(log.getId())
            .policyId(log.getPolicyId())
            .executionDate(log.getExecutionDate())
            .entityType(entityTypeStr)
            .recordsProcessed(log.getRecordsProcessed())
            .recordsArchived(log.getRecordsArchived())
            .recordsFailed(log.getRecordsFailed())
            .startTime(log.getStartTime())
            .endTime(log.getEndTime())
            .durationSeconds(log.getDurationSeconds())
            .status(log.getStatus())
            .errorMessage(log.getErrorMessage())
            .createdAt(log.getCreatedAt())
            .build();
    }
}
