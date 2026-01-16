package com.clinic.common.enums;

/**
 * Enumeration of entity types subject to data retention policies
 * Used by DataRetentionPolicy to configure retention periods for different data types
 *
 * Retention periods are based on regulatory requirements:
 * - HIPAA: 6 years minimum for healthcare records
 * - GDPR: 3 years minimum for personal data (context-specific)
 * - DPDP (India): 3 years minimum
 * - Medical Malpractice: 3-7 years depending on jurisdiction
 * - General Accounting: 7 years (SOX compliance)
 */
public enum EntityType {
    /**
     * Audit logs: Complete record of system operations and access
     * Retention: 7 years (2555 days) - HIPAA and medical malpractice requirement
     * Archival: EXPORT_TO_S3 (cold storage) after 3 months
     * Compliance: ISO 27001 A.12.4, HIPAA 45 CFR 164.312(b)
     * Note: Immutable, append-only, cannot be modified or deleted until expiration
     */
    AUDIT_LOG,

    /**
     * Patient records: Demographic data, contact information, history
     * Retention: 7 years after last visit (HIPAA)
     * Archival: EXPORT_TO_S3
     * Compliance: HIPAA 45 CFR 164.530, ISO 27001 A.18.1.3
     */
    PATIENT_RECORD,

    /**
     * Medical records: Diagnoses, treatments, medical history
     * Retention: 10 years (longer for minors until age 25)
     * Archival: EXPORT_TO_S3 with encryption
     * Compliance: HIPAA, various state medical board requirements
     * Impact: HIGH - affects future patient care
     */
    MEDICAL_RECORD,

    /**
     * Billing records: Invoices, payments, financial transactions
     * Retention: 7 years - SOX (Sarbanes-Oxley) and tax requirements
     * Archival: EXPORT_TO_S3 after 3 months
     * Compliance: IRS, SOX, HIPAA
     */
    BILLING_RECORD,

    /**
     * Appointment records: Scheduled visits, cancellations, no-shows
     * Retention: 2 years (operational, less critical than medical records)
     * Archival: SOFT_DELETE after 3 months
     * Compliance: General business record retention
     * Note: Soft delete allows recovery if needed
     */
    APPOINTMENT,

    /**
     * Prescription records: Medication orders, dispensing records
     * Retention: 3 years (for controlled substance tracking)
     * Archival: EXPORT_TO_S3 after 1 month
     * Compliance: DEA regulations, HIPAA, state pharmacy boards
     * Impact: CRITICAL - required for controlled substance audits
     */
    PRESCRIPTION,

    /**
     * Consent records: Patient consent to treatment, data sharing, medical procedures
     * Retention: 10 years (longer for legal protection)
     * Archival: ANONYMIZE after 3 years (anonymize but keep for legal proof)
     * Compliance: HIPAA, GDPR, DPDP, medical malpractice
     * Impact: CRITICAL - legal protection against malpractice claims
     * Note: When anonymized, still proves consent existed without revealing patient
     */
    CONSENT_RECORD,

    /**
     * Session records: Login sessions, authentication tokens, user sessions
     * Retention: 90 days (short-term, security-focused)
     * Archival: HARD_DELETE (truly temporary)
     * Compliance: Security best practices, ISO 27001 A.12.4.1
     * Note: Can be safely deleted as they don't contain medical data
     */
    SESSION,

    /**
     * Notification records: System notifications, alerts, emails sent
     * Retention: 30 days (informational only, not legally required)
     * Archival: HARD_DELETE
     * Compliance: General business record retention
     * Note: Can be safely deleted after brief retention period
     */
    NOTIFICATION
}
