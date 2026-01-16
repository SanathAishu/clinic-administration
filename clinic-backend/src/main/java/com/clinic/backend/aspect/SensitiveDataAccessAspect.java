package com.clinic.backend.aspect;

import com.clinic.backend.annotation.LogSensitiveAccess;
import com.clinic.backend.repository.SensitiveDataAccessLogRepository;
import com.clinic.backend.security.SecurityUtils;
import com.clinic.common.entity.BaseEntity;
import com.clinic.common.entity.compliance.SensitiveDataAccessLog;
import com.clinic.common.entity.core.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Aspect-Oriented Programming (AOP) for automatic sensitive data access logging
 *
 * Purpose: Intercept calls to methods annotated with @LogSensitiveAccess
 * and automatically create audit log entries
 *
 * Mathematical Foundation: Aspect-Oriented Design Pattern
 * - Separates cross-cutting concern (logging) from business logic
 * - Reduces code duplication and improves maintainability
 * - Ensures consistent audit logging across all sensitive operations
 *
 * How it works:
 * 1. @LogSensitiveAccess annotation is placed on service methods
 * 2. SensitiveDataAccessAspect detects these annotations via AOP
 * 3. After method execution succeeds (@AfterReturning), aspect logs the access
 * 4. Logging failures are caught and logged but don't impact application
 *
 * ISO 27001 Alignment:
 * - A.12.4.1: Event logging for information access
 * - A.12.4.2: Protection of log information
 * - Automatic, non-bypassable audit trail
 *
 * Security Considerations:
 * - Only logs on successful method execution (failed methods not logged)
 * - Captures user ID, timestamp, IP address, access type
 * - Logs are immutable and append-only (enforced by SensitiveDataAccessLog)
 * - Logging failures don't break application (graceful degradation)
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class SensitiveDataAccessAspect {

    private final SensitiveDataAccessLogRepository accessLogRepository;
    private final SecurityUtils securityUtils;
    private final HttpServletRequest request;

    /**
     * Intercept successful method execution on @LogSensitiveAccess methods
     *
     * Pointcut: Any method annotated with @LogSensitiveAccess
     * Advice: @AfterReturning - only logs on successful execution
     * Result: Automatic audit log entry creation
     *
     * @param joinPoint Method execution context
     * @param logSensitiveAccess Annotation metadata
     * @param result Method return value
     */
    @AfterReturning(
        pointcut = "@annotation(logSensitiveAccess)",
        returning = "result"
    )
    public void logSensitiveDataAccess(
        JoinPoint joinPoint,
        LogSensitiveAccess logSensitiveAccess,
        Object result) {

        try {
            // Don't log if result is null (nothing to audit)
            if (result == null) {
                return;
            }

            // Extract current context
            UUID userId = securityUtils.getCurrentUserId();
            UUID tenantId = securityUtils.getCurrentTenantId();
            String ipAddress = getClientIpAddress();
            String userAgent = request.getHeader("User-Agent");

            // Extract entity ID from method result
            UUID entityId = extractEntityId(result);
            if (entityId == null) {
                log.warn("Could not extract entity ID from method result in {}", joinPoint.getSignature());
                return;
            }

            // Create audit log entry
            SensitiveDataAccessLog accessLog = SensitiveDataAccessLog.builder()
                .entityType(logSensitiveAccess.entityType())
                .entityId(entityId)
                .accessType(logSensitiveAccess.accessType())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .accessTimestamp(Instant.now())
                .dataExported(false)
                .build();

            // Set user and tenant ID (inherited fields - must be set via setter)
            User user = new User();
            user.setId(userId);
            accessLog.setUser(user);
            accessLog.setTenantId(tenantId);

            // Extract patient ID if entity is patient-related
            try {
                if (result instanceof PatientRelated) {
                    PatientRelated patientRelated = (PatientRelated) result;
                    Object patient = patientRelated.getPatient();
                    if (patient != null) {
                        accessLog.setPatient((com.clinic.common.entity.patient.Patient) patient);
                    }
                }
            } catch (Exception e) {
                // If entity is not patient-related, no patient to log
                log.debug("Entity does not implement PatientRelated: {}", logSensitiveAccess.entityType());
            }

            // Persist audit log entry
            accessLogRepository.save(accessLog);

            log.debug(
                "Logged sensitive data access: {} {} for user {} in tenant {}",
                logSensitiveAccess.accessType(),
                entityId,
                userId,
                tenantId
            );

        } catch (Exception e) {
            // CRITICAL: Logging failures must not break application
            // Log error but continue execution
            log.error(
                "Failed to log sensitive data access for method {}: {}",
                joinPoint.getSignature(),
                e.getMessage(),
                e
            );
            // Don't rethrow - security should not break business logic
        }
    }

    /**
     * Extract client IP address from HTTP request
     * Handles proxied requests via X-Forwarded-For header
     *
     * @return Client IP address or localhost if unable to determine
     */
    private String getClientIpAddress() {
        try {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                // X-Forwarded-For can contain multiple IPs, get the first (original client)
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            log.warn("Failed to extract client IP: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * Extract entity ID from method result
     * Supports any object that extends BaseEntity
     *
     * @param result Method return value
     * @return Entity ID or null if unable to extract
     */
    private UUID extractEntityId(Object result) {
        if (result instanceof BaseEntity) {
            BaseEntity entity = (BaseEntity) result;
            return entity.getId();
        }
        return null;
    }

    /**
     * Interface for entities that are associated with a patient
     * Used to extract patient ID for audit trail filtering
     */
    public interface PatientRelated {
        Object getPatient();
    }
}
