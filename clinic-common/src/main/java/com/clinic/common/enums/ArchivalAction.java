package com.clinic.common.enums;

/**
 * Enumeration of archival actions for data retention policies
 * Used by DataRetentionPolicy to determine how old records should be handled
 *
 * Supports ISO 27001 A.18.1.3 (Protection of Records) and A.18.1.4 (Privacy and PII)
 * Each action represents different strategies for managing obsolete data.
 */
public enum ArchivalAction {
    /**
     * Soft delete: Set deletedAt timestamp, mark as inactive
     * Database: Record remains in database with soft delete marker
     * Access: Filtered out of normal queries (WHERE deletedAt IS NULL)
     * Recovery: Can be recovered by updating deletedAt back to NULL
     * Performance: No impact (record still stored)
     * Cost: Minimal (storage cost only)
     * Compliance: Not compliant with strict GDPR "right to be forgotten"
     * Use Case: Audit logs, transaction records needing long-term retention
     * Default action for most entities
     */
    SOFT_DELETE,

    /**
     * Export to S3/MinIO then delete: Archive to cold storage, remove from database
     * Database: Record deleted from hot database
     * Access: Requires explicit retrieval from archive
     * Recovery: Available through archive retrieval service
     * Performance: Improves hot database performance (smaller tables)
     * Cost: Lower (cold storage cheaper than hot database)
     * Compliance: Better GDPR compliance (removed from active system)
     * Use Case: Old patient records, historical audit logs, billing documents
     * Requires: S3/MinIO infrastructure
     */
    EXPORT_TO_S3,

    /**
     * Anonymize: Replace PII with pseudonymous values (hash, random ID)
     * Database: Record remains but PII is replaced
     * Access: Can be accessed but identification is impossible
     * Recovery: Cannot be recovered (PII permanently deleted)
     * Performance: Database size unchanged
     * Cost: Minimal (only storage, no archive)
     * Compliance: GDPR "right to be forgotten" compliant (de-identified data)
     * Use Case: Patient consent records, contact information after consent revocation
     * Security: Irreversible - choose carefully
     */
    ANONYMIZE,

    /**
     * Hard delete: Permanently remove record from database
     * Database: Record completely deleted (PURGE)
     * Access: Impossible to recover
     * Recovery: None - data is gone
     * Performance: Reduces database size (best for cleanup)
     * Cost: Minimal (less storage)
     * Compliance: Strongest GDPR compliance
     * Use Case: Temporary data (sessions, notifications, temporary audit entries)
     * Danger: Irreversible - audit trail lost
     * WARNING: Only use for non-critical, non-audit data
     */
    HARD_DELETE
}
