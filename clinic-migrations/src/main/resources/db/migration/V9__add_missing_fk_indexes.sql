-- ============================================
-- Migration: V9__add_missing_fk_indexes
-- Description: Add indexes on foreign key columns for query performance
-- Impact: Improves JOIN performance, CASCADE operations, and referential integrity checks
-- ============================================

-- ========================================
-- CRITICAL INDEXES (High-Impact on Query Performance)
-- ========================================

-- Index: billing.appointment_id
-- Use case: Queries billing records by appointment (very frequent)
-- Example: SELECT * FROM billing WHERE appointment_id = ?
CREATE INDEX idx_billing_appointment ON billing(appointment_id)
WHERE deleted_at IS NULL;

-- Index: lab_tests.medical_record_id
-- Use case: Retrieve all lab tests for a medical record (very frequent)
-- Example: SELECT * FROM lab_tests WHERE medical_record_id = ?
CREATE INDEX idx_lab_tests_medical_record ON lab_tests(medical_record_id)
WHERE deleted_at IS NULL;

-- Index: medical_records.doctor_id
-- Use case: Retrieve all medical records by doctor (frequent - doctor dashboards)
-- Example: SELECT * FROM medical_records WHERE doctor_id = ? ORDER BY record_date DESC
CREATE INDEX idx_medical_records_doctor ON medical_records(doctor_id, record_date DESC)
WHERE deleted_at IS NULL;

-- Index: prescriptions.doctor_id
-- Use case: Retrieve all prescriptions by doctor (frequent - doctor dashboards)
-- Example: SELECT * FROM prescriptions WHERE doctor_id = ? ORDER BY created_at DESC
CREATE INDEX idx_prescriptions_doctor ON prescriptions(doctor_id, created_at DESC)
WHERE deleted_at IS NULL;

-- ========================================
-- FREQUENTLY QUERIED AUDIT FIELDS
-- ========================================

-- Index: lab_tests.ordered_by
-- Use case: Doctors querying their lab test orders, audit reports
-- Example: SELECT * FROM lab_tests WHERE ordered_by = ? AND status = 'PENDING'
CREATE INDEX idx_lab_tests_ordered_by ON lab_tests(ordered_by, status)
WHERE deleted_at IS NULL;

-- Index: prescriptions.created_by
-- Use case: Audit trails, prescription reports by staff member
-- Example: SELECT * FROM prescriptions WHERE created_by = ? AND created_at > ?
CREATE INDEX idx_prescriptions_created_by ON prescriptions(created_by, created_at DESC)
WHERE deleted_at IS NULL;

-- Index: medical_records.created_by
-- Use case: Audit trails, medical record reports by staff member
-- Example: SELECT * FROM medical_records WHERE created_by = ? AND created_at > ?
CREATE INDEX idx_medical_records_created_by ON medical_records(created_by, created_at DESC)
WHERE deleted_at IS NULL;

-- Index: billing.created_by
-- Use case: Billing reports by staff member, audit trails
-- Example: SELECT * FROM billing WHERE created_by = ? AND invoice_date BETWEEN ? AND ?
CREATE INDEX idx_billing_created_by ON billing(created_by, invoice_date DESC)
WHERE deleted_at IS NULL;

-- ========================================
-- ADDITIONAL USEFUL INDEXES
-- ========================================

-- Index: appointments.created_by
-- Use case: Appointment booking reports by staff
-- Example: SELECT COUNT(*) FROM appointments WHERE created_by = ? AND created_at > ?
CREATE INDEX idx_appointments_created_by ON appointments(created_by, created_at DESC)
WHERE deleted_at IS NULL;

-- Index: appointments.cancelled_by
-- Use case: Cancellation reports, audit who cancelled appointments
-- Example: SELECT * FROM appointments WHERE cancelled_by = ? AND cancelled_at > ?
CREATE INDEX idx_appointments_cancelled_by ON appointments(cancelled_by, cancelled_at DESC)
WHERE cancelled_at IS NOT NULL AND deleted_at IS NULL;

-- ========================================
-- INDEX STATISTICS
-- ========================================

-- Comment for documentation
COMMENT ON INDEX idx_billing_appointment IS 'FK index for billing.appointment_id - improves JOIN and CASCADE performance';
COMMENT ON INDEX idx_lab_tests_medical_record IS 'FK index for lab_tests.medical_record_id - improves JOIN and CASCADE performance';
COMMENT ON INDEX idx_medical_records_doctor IS 'FK index for medical_records.doctor_id - doctor dashboard queries';
COMMENT ON INDEX idx_prescriptions_doctor IS 'FK index for prescriptions.doctor_id - doctor dashboard queries';
COMMENT ON INDEX idx_lab_tests_ordered_by IS 'FK index for lab_tests.ordered_by - audit and doctor activity queries';
COMMENT ON INDEX idx_prescriptions_created_by IS 'FK index for prescriptions.created_by - audit trail queries';
COMMENT ON INDEX idx_medical_records_created_by IS 'FK index for medical_records.created_by - audit trail queries';
COMMENT ON INDEX idx_billing_created_by IS 'FK index for billing.created_by - billing reports and audit';
COMMENT ON INDEX idx_appointments_created_by IS 'FK index for appointments.created_by - booking reports and audit';
COMMENT ON INDEX idx_appointments_cancelled_by IS 'FK index for appointments.cancelled_by - cancellation reports';

-- Analysis: Verify index usage
-- Run these queries after migration to monitor index effectiveness:
-- SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
-- FROM pg_stat_user_indexes
-- WHERE indexname LIKE 'idx_%_doctor' OR indexname LIKE 'idx_%_created_by'
-- ORDER BY idx_scan DESC;
