package com.clinic.backend.controller;

import com.clinic.common.entity.compliance.SensitiveDataAccessLog;
import com.clinic.common.enums.AccessType;
import com.clinic.backend.dto.compliance.AccessAuditDTO;
import com.clinic.backend.service.AccessAuditReportingService;
import com.clinic.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Access Audit Trail and Sensitive Data Logging
 *
 * Provides endpoints for querying sensitive data access logs and
 * generating compliance audit reports. ISO 27001 A.12.4 aligned.
 *
 * Security: Requires ADMIN or COMPLIANCE_OFFICER role
 */
@RestController
@RequestMapping("/api/access-audit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN', 'COMPLIANCE_OFFICER')")
@Slf4j
public class AccessAuditController {

    private final AccessAuditReportingService auditService;
    private final SecurityUtils securityUtils;

    /**
     * GET /api/access-audit/patient/{patientId}
     * Get all access logs for specific patient
     * HIPAA 164.308(a)(7)(ii) compliance: patient access accountability
     *
     * @param patientId Patient ID
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Paginated list of access logs
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<Page<AccessAuditDTO>> getPatientAccessLogs(
            @PathVariable UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID tenantId = securityUtils.getCurrentTenantId();
        LocalDate actualStart = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate actualEnd = endDate != null ? endDate : LocalDate.now();

        log.info("Fetching access logs for patient {} in tenant {} from {} to {}",
            patientId, tenantId, actualStart, actualEnd);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<SensitiveDataAccessLog> logs = auditService.getPatientAccessLogs(patientId, tenantId,
                actualStart, actualEnd, pageable);

            Page<AccessAuditDTO> dtos = logs.map(this::toDTO);
            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Error fetching patient access logs: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/access-audit/user/{userId}
     * Get all sensitive data access by specific user
     *
     * @param userId User ID
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Paginated list of access logs
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AccessAuditDTO>> getUserAccessLogs(
            @PathVariable UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID tenantId = securityUtils.getCurrentTenantId();
        LocalDate actualStart = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate actualEnd = endDate != null ? endDate : LocalDate.now();

        log.info("Fetching access logs for user {} in tenant {} from {} to {}",
            userId, tenantId, actualStart, actualEnd);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<SensitiveDataAccessLog> logs = auditService.getUserAccessLogs(userId, tenantId,
                actualStart, actualEnd, pageable);

            Page<AccessAuditDTO> dtos = logs.map(this::toDTO);
            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Error fetching user access logs: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/access-audit/recent?days=7
     * Get recent sensitive data access
     *
     * @param days Number of days to look back (default: 7)
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Paginated list of recent access logs
     */
    @GetMapping("/recent")
    public ResponseEntity<Page<AccessAuditDTO>> getRecentAccessLogs(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID tenantId = securityUtils.getCurrentTenantId();
        log.info("Fetching recent access logs for tenant {} since {} days ago", tenantId, days);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<SensitiveDataAccessLog> logs = auditService.getRecentAccessLogs(tenantId, days, pageable);

            Page<AccessAuditDTO> dtos = logs.map(this::toDTO);
            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Error fetching recent access logs: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/access-audit/exports
     * Get all data export operations (high-risk access pattern)
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Paginated list of export operations
     */
    @GetMapping("/exports")
    public ResponseEntity<Page<AccessAuditDTO>> getExportOperations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID tenantId = securityUtils.getCurrentTenantId();
        LocalDate actualStart = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate actualEnd = endDate != null ? endDate : LocalDate.now();

        log.info("Fetching data export operations for tenant {} from {} to {}", tenantId, actualStart, actualEnd);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<SensitiveDataAccessLog> logs = auditService.getDataExportOperations(tenantId,
                actualStart, actualEnd, pageable);

            Page<AccessAuditDTO> dtos = logs.map(this::toDTO);
            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Error fetching export operations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/access-audit/entity/{type}/{id}
     * Get complete access history for specific entity
     *
     * @param type Entity type (MedicalRecord, Prescription, etc.)
     * @param id Entity ID
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Paginated list of entity access logs
     */
    @GetMapping("/entity/{type}/{id}")
    public ResponseEntity<Page<AccessAuditDTO>> getEntityAccessLogs(
            @PathVariable String type,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID tenantId = securityUtils.getCurrentTenantId();
        log.info("Fetching access logs for {} {} in tenant {}", type, id, tenantId);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<SensitiveDataAccessLog> logs = auditService.getEntityAccessLogs(type, id, tenantId, pageable);

            Page<AccessAuditDTO> dtos = logs.map(this::toDTO);
            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Error fetching entity access logs: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/access-audit/by-type/{accessType}
     * Get access logs filtered by operation type
     *
     * @param accessType Type of access (VIEW_MEDICAL_RECORD, EXPORT_PATIENT_DATA, etc.)
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Paginated list of logs for access type
     */
    @GetMapping("/by-type/{accessType}")
    public ResponseEntity<Page<AccessAuditDTO>> getAccessLogsByType(
            @PathVariable String accessType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID tenantId = securityUtils.getCurrentTenantId();
        LocalDate actualStart = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate actualEnd = endDate != null ? endDate : LocalDate.now();

        log.info("Fetching access logs for type {} in tenant {} from {} to {}",
            accessType, tenantId, actualStart, actualEnd);

        try {
            AccessType type = AccessType.valueOf(accessType);
            Pageable pageable = PageRequest.of(page, size);
            Page<SensitiveDataAccessLog> logs = auditService.getAccessLogsByType(type, tenantId,
                actualStart, actualEnd, pageable);

            Page<AccessAuditDTO> dtos = logs.map(this::toDTO);
            return ResponseEntity.ok(dtos);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid access type: {}", accessType);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching access logs by type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/access-audit/suspicious
     * Detect suspicious access patterns (anomaly detection)
     *
     * @return List of suspicious patterns detected
     */
    @GetMapping("/suspicious")
    public ResponseEntity<List<Map<String, Object>>> getSuspiciousPatterns() {
        UUID tenantId = securityUtils.getCurrentTenantId();
        log.info("Detecting suspicious access patterns for tenant {}", tenantId);

        try {
            List<Map<String, Object>> patterns = auditService.detectSuspiciousPatterns(tenantId);
            return ResponseEntity.ok(patterns);

        } catch (Exception e) {
            log.error("Error detecting suspicious patterns: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/access-audit/report?startDate=2025-01-01&endDate=2025-01-31
     * Generate comprehensive access audit report
     *
     * @param startDate Report start date (default: 30 days ago)
     * @param endDate Report end date (default: today)
     * @return Audit report with statistics
     */
    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> generateAuditReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        UUID tenantId = securityUtils.getCurrentTenantId();
        LocalDate actualStart = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate actualEnd = endDate != null ? endDate : LocalDate.now();

        log.info("Generating audit report for tenant {} from {} to {}", tenantId, actualStart, actualEnd);

        try {
            Map<String, Object> report = auditService.generateAuditReport(tenantId, actualStart, actualEnd);
            return ResponseEntity.ok(report);

        } catch (Exception e) {
            log.error("Error generating audit report: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/access-audit/patient/{patientId}/complete
     * Get complete audit trail for patient (all-time history)
     * Compliance: Complete patient access accountability
     *
     * @param patientId Patient ID
     * @return Complete access history for patient
     */
    @GetMapping("/patient/{patientId}/complete")
    public ResponseEntity<List<AccessAuditDTO>> getCompletePatientAuditTrail(
            @PathVariable UUID patientId) {

        UUID tenantId = securityUtils.getCurrentTenantId();
        log.info("Fetching complete audit trail for patient {} in tenant {}", patientId, tenantId);

        try {
            List<SensitiveDataAccessLog> logs = auditService.getCompletePatientAuditTrail(patientId, tenantId);
            List<AccessAuditDTO> dtos = logs.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            log.error("Error fetching complete audit trail: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Helper: Convert SensitiveDataAccessLog to DTO
     */
    private AccessAuditDTO toDTO(SensitiveDataAccessLog log) {
        return AccessAuditDTO.builder()
            .id(log.getId())
            .userId(log.getUser() != null ? log.getUser().getId() : null)
            .patientId(log.getPatient() != null ? log.getPatient().getId() : null)
            .entityType(log.getEntityType())
            .entityId(log.getEntityId())
            .accessType(log.getAccessType())
            .accessTimestamp(log.getAccessTimestamp())
            .ipAddress(log.getIpAddress())
            .userAgent(log.getUserAgent())
            .accessReason(log.getAccessReason())
            .dataExported(log.getDataExported())
            .createdAt(log.getCreatedAt())
            .build();
    }
}
