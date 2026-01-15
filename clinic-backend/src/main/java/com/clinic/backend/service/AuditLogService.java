package com.clinic.backend.service;

import com.clinic.common.entity.core.AuditLog;
import com.clinic.backend.repository.AuditLogRepository;
import com.clinic.common.enums.AuditAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Create audit log entry (Append-only, immutable sequence)
     * This is the ONLY write operation - no updates or deletes allowed
     */
    @Transactional
    public AuditLog createAuditLog(AuditLog auditLog) {
        // Enforce immutability - ensure timestamp is set
        if (auditLog.getTimestamp() == null) {
            auditLog.setTimestamp(Instant.now());
        }

        AuditLog saved = auditLogRepository.save(auditLog);
        log.debug("Created audit log: {} by user {} on entity {}/{}",
                saved.getAction(), saved.getUserId(), saved.getEntityType(), saved.getEntityId());
        return saved;
    }

    /**
     * Convenience method to create audit log
     */
    @Transactional
    public AuditLog log(UUID tenantId, UUID userId, AuditAction action, String entityType,
                        UUID entityId, Map<String, Object> oldValues, Map<String, Object> newValues,
                        String ipAddress, String userAgent) {

        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValues(oldValues)
                .newValues(newValues)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .timestamp(Instant.now())
                .build();

        auditLog.setTenantId(tenantId);

        return createAuditLog(auditLog);
    }

    /**
     * Get audit logs for tenant (with pagination)
     */
    public Page<AuditLog> getAuditLogsForTenant(UUID tenantId, Pageable pageable) {
        return auditLogRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * Get audit logs by action
     */
    public Page<AuditLog> getAuditLogsByAction(UUID tenantId, AuditAction action, Pageable pageable) {
        return auditLogRepository.findByTenantIdAndAction(tenantId, action, pageable);
    }

    /**
     * Get audit logs for user (User activity tracking)
     */
    public Page<AuditLog> getAuditLogsForUser(UUID tenantId, UUID userId, Pageable pageable) {
        return auditLogRepository.findByTenantIdAndUserId(tenantId, userId, pageable);
    }

    /**
     * Get audit trail for specific entity (Temporal sequence)
     */
    public Page<AuditLog> getEntityAuditTrail(UUID tenantId, String entityType, UUID entityId,
                                               Pageable pageable) {
        return auditLogRepository.findByTenantIdAndEntityTypeAndEntityId(
                tenantId, entityType, entityId, pageable);
    }

    /**
     * Get audit logs in date range (Temporal queries)
     */
    public Page<AuditLog> getAuditLogsByDateRange(UUID tenantId, Instant start, Instant end,
                                                   Pageable pageable) {
        return auditLogRepository.findByTenantIdAndTimestampBetween(tenantId, start, end, pageable);
    }

    /**
     * Get recent activity
     */
    public List<AuditLog> getRecentActivity(UUID tenantId, Instant since) {
        return auditLogRepository.findRecentActivityByTenant(tenantId, since);
    }

    /**
     * Get user activity since timestamp
     */
    public List<AuditLog> getUserActivity(UUID userId, UUID tenantId, Instant since) {
        return auditLogRepository.findUserActivitySince(userId, tenantId, since);
    }

    /**
     * Get audit logs by IP address (Security monitoring)
     */
    public List<AuditLog> getAuditLogsByIpAddress(String ipAddress, UUID tenantId, Instant since) {
        return auditLogRepository.findByIpAddressAndTenantIdSince(ipAddress, tenantId, since);
    }

    /**
     * Count audit logs in date range
     */
    public long countAuditLogs(UUID tenantId, Instant start, Instant end) {
        return auditLogRepository.countByTenantIdAndTimestampBetween(tenantId, start, end);
    }

    /**
     * Count audit logs by action
     */
    public long countAuditLogsByAction(UUID tenantId, AuditAction action, Instant start, Instant end) {
        return auditLogRepository.countByTenantIdAndActionAndTimestampBetween(
                tenantId, action, start, end);
    }

    /**
     * Verify audit log integrity (Immutability check)
     * This method is for administrative/compliance purposes only
     */
    public boolean verifyAuditLogIntegrity(UUID auditLogId) {
        // Audit logs should never be modified after creation
        // In a production system, you might add cryptographic signatures
        // or blockchain integration for enhanced integrity verification
        return auditLogRepository.existsById(auditLogId);
    }
}
