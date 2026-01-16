package com.clinic.backend.service;

import com.clinic.common.entity.compliance.DataRetentionPolicy;
import com.clinic.common.entity.compliance.DataArchivalLog;
import com.clinic.common.entity.core.AuditLog;
import com.clinic.common.entity.operational.Notification;
import com.clinic.common.entity.core.Session;
import com.clinic.common.enums.EntityType;
import com.clinic.common.enums.ArchivalAction;
import com.clinic.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Data Retention Service - Automated data lifecycle management
 *
 * Executes retention policies to archive, anonymize, or delete records
 * based on configurable retention periods. ISO 27001 A.18.1.3 aligned.
 *
 * Mathematical Foundation: Data Lifecycle Management
 *
 * Archive Date = Created Date + Retention Period
 * Delete Date = Archive Date + Grace Period
 *
 * Pareto Principle for Data:
 * 80% of queries access 20% of data (recent records)
 * Archival of old data reduces storage costs without performance impact
 *
 * Archival Actions:
 * 1. SOFT_DELETE: Set deletedAt timestamp (reversible, default)
 * 2. EXPORT_TO_S3: Export to MinIO then delete (audit trail preserved)
 * 3. ANONYMIZE: Replace PII with pseudonymous values (GDPR compliance)
 * 4. HARD_DELETE: Permanent deletion (only for non-critical data)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DataRetentionService {

    private final DataRetentionPolicyRepository policyRepository;
    private final DataArchivalLogRepository archivalLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;
    private final SessionRepository sessionRepository;
    private final EmailService emailService;

    /**
     * Execute data retention policies daily at 2:00 AM UTC.
     * Archives/deletes records that exceed retention period + grace period.
     *
     * Invariants enforced:
     * 1. records_archived ≤ records_processed
     * 2. records_processed = records_archived + records_failed
     * 3. end_time ≥ start_time
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    public void executeRetentionPolicies() {
        log.info("Starting data retention policy execution");

        try {
            // Query all enabled retention policies (placeholder - actual tenant context needed)
            List<DataRetentionPolicy> policies = List.of();

            if (policies.isEmpty()) {
                log.info("No active retention policies found");
                return;
            }

            log.info("Found {} active retention policies to execute", policies.size());

            for (DataRetentionPolicy policy : policies) {
                try {
                    executePolicy(policy);
                } catch (Exception e) {
                    log.error("Failed to execute retention policy for {}: {}",
                        policy.getEntityType(), e.getMessage(), e);
                }
            }

            log.info("Completed data retention policy execution");

        } catch (Exception e) {
            log.error("Error during retention policy execution: {}", e.getMessage(), e);
        }
    }

    /**
     * Execute a single retention policy.
     * Creates archival log entry and updates policy with execution timestamp.
     */
    private void executePolicy(DataRetentionPolicy policy) {
        Instant cutoffDate = Instant.now()
            .minus(policy.getRetentionDays(), java.time.temporal.ChronoUnit.DAYS);

        Instant deleteDate = cutoffDate
            .minus(policy.getGracePeriodDays(), java.time.temporal.ChronoUnit.DAYS);

        DataArchivalLog archivalLog = DataArchivalLog.builder()
            .policyId(policy.getId())
            .tenantId(policy.getTenantId())
            .executionDate(LocalDate.now())
            .entityType(policy.getEntityType())
            .startTime(Instant.now())
            .status("RUNNING")
            .recordsProcessed(0L)
            .recordsArchived(0L)
            .recordsFailed(0L)
            .build();

        archivalLogRepository.save(archivalLog);

        try {
            long archived = archiveRecords(policy, cutoffDate, deleteDate);

            archivalLog.setRecordsArchived(archived);
            archivalLog.setRecordsProcessed(archived);
            archivalLog.setStatus("COMPLETED");
            archivalLog.setEndTime(Instant.now());

            long durationSeconds = Duration.between(archivalLog.getStartTime(), archivalLog.getEndTime())
                .getSeconds();
            archivalLog.setDurationSeconds((int) durationSeconds);

            policy.setLastExecution(Instant.now());
            policy.setRecordsArchived(policy.getRecordsArchived() + archived);
            policyRepository.save(policy);
            archivalLogRepository.save(archivalLog);

            log.info("Archived {} {} records for tenant {} (archival action: {})",
                archived, policy.getEntityType(), policy.getTenantId(), policy.getArchivalAction());

            // Send notification to compliance team
            emailService.sendRetentionExecutionNotification(
                policy.getEntityType().toString(),
                archived,
                "COMPLETED",
                null
            );

        } catch (Exception e) {
            archivalLog.setStatus("FAILED");
            archivalLog.setEndTime(Instant.now());
            archivalLog.setErrorMessage(e.getMessage());
            archivalLogRepository.save(archivalLog);

            log.error("Failed to execute retention policy for {}: {}",
                policy.getEntityType(), e.getMessage());

            emailService.sendRetentionExecutionNotification(
                policy.getEntityType().toString(),
                0,
                "FAILED",
                e.getMessage()
            );

            throw e;
        }
    }

    /**
     * Execute archival action based on policy configuration.
     * Routes to specific archival methods by entity type.
     */
    private long archiveRecords(DataRetentionPolicy policy, Instant cutoffDate, Instant deleteDate) {
        log.debug("Archiving {} records created before {} (action: {})",
            policy.getEntityType(), cutoffDate, policy.getArchivalAction());

        return switch (policy.getEntityType()) {
            case AUDIT_LOG -> archiveAuditLogs(policy, cutoffDate, deleteDate);
            case SESSION -> archiveSessions(policy, cutoffDate, deleteDate);
            case NOTIFICATION -> archiveNotifications(policy, cutoffDate, deleteDate);
            default -> {
                log.warn("No archival implementation for entity type: {}", policy.getEntityType());
                yield 0L;
            }
        };
    }

    /**
     * Archive audit logs based on archival action (placeholder implementation).
     */
    private long archiveAuditLogs(DataRetentionPolicy policy, Instant cutoffDate, Instant deleteDate) {
        log.debug("Archiving audit logs (placeholder implementation)");
        // Placeholder: actual implementation deferred
        return 0L;
    }

    /**
     * Archive sessions based on archival action (placeholder implementation).
     */
    private long archiveSessions(DataRetentionPolicy policy, Instant cutoffDate, Instant deleteDate) {
        log.debug("Archiving sessions (placeholder implementation)");
        // Placeholder: actual implementation deferred
        return 0L;
    }

    /**
     * Archive notifications based on archival action (placeholder implementation).
     */
    private long archiveNotifications(DataRetentionPolicy policy, Instant cutoffDate, Instant deleteDate) {
        log.debug("Archiving notifications (placeholder implementation)");
        // Placeholder: actual implementation deferred
        return 0L;
    }

    /**
     * Get retention policy for entity type.
     */
    @Transactional(readOnly = true)
    public DataRetentionPolicy getPolicyForEntityType(EntityType entityType, UUID tenantId) {
        return policyRepository.findByEntityType(tenantId, entityType)
            .orElse(null);
    }

    /**
     * Get archival execution logs for date range (placeholder).
     */
    @Transactional(readOnly = true)
    public List<DataArchivalLog> getArchivalLogs(LocalDate startDate, LocalDate endDate, UUID tenantId) {
        log.debug("Fetching archival logs from {} to {}", startDate, endDate);
        // Placeholder: actual implementation deferred
        return List.of();
    }

    /**
     * Get recent successful archival executions (placeholder).
     */
    @Transactional(readOnly = true)
    public List<DataArchivalLog> getSuccessfulArchivalLogs(int days, UUID tenantId) {
        log.debug("Fetching successful archival logs for last {} days", days);
        // Placeholder: actual implementation deferred
        return List.of();
    }

    /**
     * Get failed archival executions requiring attention (placeholder).
     */
    @Transactional(readOnly = true)
    public List<DataArchivalLog> getFailedArchivalLogs(UUID tenantId) {
        log.debug("Fetching failed archival logs");
        // Placeholder: actual implementation deferred
        return List.of();
    }
}
