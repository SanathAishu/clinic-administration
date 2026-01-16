package com.clinic.common.enums;

/**
 * Enumeration of sensitive data access types for audit trail logging
 * Used by SensitiveDataAccessLog to track access to protected information
 *
 * Supports ISO 27001 A.12.4 (Logging & Monitoring) and HIPAA audit trail requirements.
 * Each access type represents an operation on sensitive patient or medical data.
 */
public enum AccessType {
    /**
     * View entire medical record including history, diagnoses, treatments
     * Sensitive: Yes - PII and protected health information
     * Audit: MUST be logged per ISO 27001 A.12.4.1
     */
    VIEW_MEDICAL_RECORD,

    /**
     * View prescription details including medications and dosages
     * Sensitive: Yes - Protected health information
     * Audit: MUST be logged for controlled substance tracking
     */
    VIEW_PRESCRIPTION,

    /**
     * View laboratory test results
     * Sensitive: Yes - Sensitive health information
     * Audit: MUST be logged
     */
    VIEW_LAB_RESULT,

    /**
     * View patient personal details (name, DOB, address, contact)
     * Sensitive: Yes - PII
     * Audit: MUST be logged per ISO 27001 A.18.1.4 (Privacy and PII)
     */
    VIEW_PATIENT_DETAILS,

    /**
     * Export complete patient data (bulk download/export)
     * Sensitive: Yes - Complete PII and PHI
     * Audit: MUST be logged with flag (dataExported = true)
     * Note: Requires explicit authorization and justified access reason
     */
    EXPORT_PATIENT_DATA,

    /**
     * Modify medical record (add diagnosis, update treatment)
     * Sensitive: Yes - Creates audit trail for medical decision changes
     * Audit: MUST be logged with user attribution
     * Impact: High - affects future treatment decisions
     */
    MODIFY_MEDICAL_RECORD,

    /**
     * Print prescription (physical copy generation)
     * Sensitive: Yes - Physical document distribution
     * Audit: MUST be logged to track controlled substance prescriptions
     */
    PRINT_PRESCRIPTION,

    /**
     * View billing and payment details
     * Sensitive: Yes - Financial PII
     * Audit: MUST be logged per ISO 27001 A.18.1.4
     */
    VIEW_BILLING_DETAILS
}
