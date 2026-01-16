package com.clinic.backend.annotation;

import com.clinic.common.enums.AccessType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for declarative sensitive data access logging
 *
 * Purpose: Mark service methods that access sensitive patient data for automatic audit logging
 * Mechanism: Used by SensitiveDataAccessAspect to intercept method calls
 *
 * Usage:
 * @LogSensitiveAccess(entityType = "MedicalRecord", accessType = AccessType.VIEW_MEDICAL_RECORD)
 * public MedicalRecord getMedicalRecordById(UUID id, UUID tenantId) {
 *     // Log entry is automatically created after successful method execution
 * }
 *
 * ISO 27001 Alignment:
 * - A.12.4.1: Event logging for user access to information
 * - Provides declarative, compile-time safe audit logging
 * - No manual logging code required in service methods
 *
 * Security Considerations:
 * - Annotation is processed by Spring AOP aspect
 * - Logging occurs AFTER successful method execution (@AfterReturning)
 * - Logging failures do not impact application flow (logged but not thrown)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogSensitiveAccess {
    /**
     * Type of entity being accessed (MedicalRecord, Prescription, LabResult, etc.)
     * Used for audit trail filtering and categorization
     */
    String entityType();

    /**
     * Type of access operation (VIEW, EXPORT, MODIFY, PRINT, etc.)
     * Defines the nature of the sensitive operation
     */
    AccessType accessType();

    /**
     * Optional: Business justification required?
     * If true, users should provide accessReason in SensitiveDataAccessLog
     * Default: false (reason is optional)
     */
    boolean requireReason() default false;
}
