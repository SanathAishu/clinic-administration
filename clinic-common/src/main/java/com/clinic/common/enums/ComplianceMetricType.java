package com.clinic.common.enums;

/**
 * Enumeration of compliance metric types for monitoring and alerting
 * Based on ISO 27001 control objectives
 *
 * Used in ComplianceMetrics entity for tracking different aspects of system health:
 * - Queue stability (M/M/1 queuing theory)
 * - SLA adherence (Service Level Agreements)
 * - System performance metrics
 * - Security compliance metrics
 */
public enum ComplianceMetricType {
    /**
     * Queue Stability: Monitors M/M/1 queuing system stability
     * Metric: Percent of queues with ρ < 1.0 (stable)
     * Target: 100% of queues stable
     * SLA: All doctors should maintain ρ < 0.85
     */
    QUEUE_STABILITY,

    /**
     * Wait Time SLA: Monitors appointment wait times
     * Metric: Percent of appointments meeting wait time targets
     * Target: >= 95% compliance
     * SLA: Average wait time < 15 minutes for scheduled appointments
     */
    WAIT_TIME_SLA,

    /**
     * Cache Hit Rate: Monitors distributed cache effectiveness
     * Metric: Percent of requests served from cache vs database
     * Target: >= 80% hit rate
     * SLA: Sustained >= 75% during peak hours
     */
    CACHE_HIT_RATE,

    /**
     * Error Rate: Monitors API error rate
     * Metric: Percent of requests resulting in 4xx/5xx errors
     * Target: < 0.1% error rate
     * SLA: < 0.5% (99.5% success rate)
     */
    ERROR_RATE,

    /**
     * Access Log Coverage: Monitors audit trail completeness
     * Metric: Percent of sensitive operations that are logged
     * Target: 100% coverage
     * SLA: All CRUD operations on medical records must be logged (ISO 27001 A.12.4.1)
     */
    ACCESS_LOG_COVERAGE,

    /**
     * Data Retention Compliance: Monitors adherence to retention policies
     * Metric: Percent of records within retention policy
     * Target: 100% compliance
     * SLA: All records should be archived/deleted according to policy (ISO 27001 A.18.1.3)
     */
    DATA_RETENTION_COMPLIANCE,

    /**
     * Consent Validity: Monitors valid patient consents
     * Metric: Percent of active consents not expired
     * Target: >= 98% validity
     * SLA: All patient interactions require valid consent (GDPR/HIPAA)
     */
    CONSENT_VALIDITY
}
