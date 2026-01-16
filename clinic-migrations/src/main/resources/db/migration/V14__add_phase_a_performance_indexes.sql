-- ============================================================================
-- V14: Add Performance Indexes for Phase A Tables
-- ============================================================================
-- This migration adds strategic indexes to optimize query performance
-- for Phase A entities (Branch, Treatment, MedicalOrder) and enhancements
--
-- Index Strategy:
-- 1. Foreign Key indexes (JOIN optimization, CASCADE operations)
-- 2. Filter indexes (WHERE clauses on frequently queried columns)
-- 3. Composite indexes (multi-column queries)
-- 4. Partial indexes (WHERE deleted_at IS NULL, filtered status values)
--
-- Operations Research Principle: Query optimization for resource allocation
-- ============================================================================

-- ============================================================================
-- BRANCH TABLE ADDITIONAL INDEXES
-- ============================================================================

-- Index: created_by for audit trails and staff reports
-- Use case: Reports on branches created by specific staff
-- Example: SELECT * FROM branches WHERE created_by = ? AND created_at > ?
CREATE INDEX idx_branches_created_by ON branches(created_by, created_at DESC)
WHERE deleted_at IS NULL;

-- Composite index for common queries: tenant + active status
-- Use case: Get active branches for a tenant
-- Example: SELECT * FROM branches WHERE tenant_id = ? AND is_active = true
CREATE INDEX idx_branches_tenant_active ON branches(tenant_id, is_active)
WHERE deleted_at IS NULL;

-- ============================================================================
-- TREATMENT TABLE ADDITIONAL INDEXES
-- ============================================================================

-- Index: created_by for audit trails and staff reports
-- Use case: Treatment catalog management by staff
-- Example: SELECT * FROM treatments WHERE created_by = ? ORDER BY created_at DESC
CREATE INDEX idx_treatments_created_by ON treatments(created_by, created_at DESC)
WHERE deleted_at IS NULL;

-- Composite index for pricing analysis
-- Use case: Cost analysis queries across treatments
-- Example: SELECT * FROM treatments WHERE base_cost BETWEEN ? AND ? AND is_active = true
CREATE INDEX idx_treatments_cost_active ON treatments(base_cost, is_active)
WHERE deleted_at IS NULL;

-- Composite index for category browsing
-- Use case: List active treatments by category with tenant isolation
-- Example: SELECT * FROM treatments WHERE tenant_id = ? AND category = ? AND is_active = true
CREATE INDEX idx_treatments_tenant_category_active ON treatments(tenant_id, category, is_active)
WHERE deleted_at IS NULL;

-- ============================================================================
-- MEDICAL ORDER TABLE ADDITIONAL INDEXES
-- ============================================================================

-- Index: created_by for audit trails and ordering history
-- Use case: Orders placed by specific staff members
-- Example: SELECT * FROM medical_orders WHERE created_by = ? AND created_at > ?
CREATE INDEX idx_medical_orders_created_by ON medical_orders(created_by, order_date DESC)
WHERE deleted_at IS NULL;

-- Composite index for patient order history
-- Use case: Get all orders for a patient, sorted by date
-- Example: SELECT * FROM medical_orders WHERE patient_id = ? AND tenant_id = ? ORDER BY order_date DESC
CREATE INDEX idx_medical_orders_patient_date ON medical_orders(patient_id, tenant_id, order_date DESC)
WHERE deleted_at IS NULL;

-- Composite index for status filtering within tenant
-- Use case: Dashboard queries for pending orders, shipments in transit, etc.
-- Example: SELECT * FROM medical_orders WHERE tenant_id = ? AND status IN (PENDING, SENT, IN_PRODUCTION)
CREATE INDEX idx_medical_orders_tenant_status ON medical_orders(tenant_id, status)
WHERE deleted_at IS NULL;

-- Index for overdue order detection (Feature: find orders past expected delivery)
-- Use case: Automated alerts for overdue orders
-- Example: SELECT * FROM medical_orders WHERE expected_delivery_date < ? AND status NOT IN (DELIVERED, CANCELLED)
CREATE INDEX idx_medical_orders_overdue ON medical_orders(expected_delivery_date, status)
WHERE expected_delivery_date IS NOT NULL AND
      status NOT IN ('DELIVERED', 'CANCELLED') AND
      deleted_at IS NULL;

-- Index for delivery date analysis (analytics)
-- Use case: Lead time analysis, delivery performance reports
-- Example: SELECT * FROM medical_orders WHERE order_date BETWEEN ? AND ? ORDER BY actual_delivery_date
CREATE INDEX idx_medical_orders_delivery_dates ON medical_orders(order_date, actual_delivery_date)
WHERE deleted_at IS NULL;

-- ============================================================================
-- APPOINTMENT TABLE ENHANCEMENT INDEXES
-- ============================================================================

-- Composite index for doctor-branch scheduling
-- Use case: Check doctor's schedule at specific branch
-- Example: SELECT * FROM appointments WHERE doctor_id = ? AND branch_id = ? AND appointment_time BETWEEN ? AND ?
CREATE INDEX idx_appointments_doctor_branch_time_detailed ON appointments(
    doctor_id, branch_id, appointment_time, status
)
WHERE deleted_at IS NULL;

-- Index for token number generation (Sequences: sequential tokens)
-- Use case: Generate next token number for doctor on given day
-- Example: SELECT MAX(token_number) FROM appointments WHERE doctor_id = ? AND DATE(appointment_time) = ?
CREATE INDEX idx_appointments_doctor_token_by_date ON appointments(
    doctor_id, appointment_time DESC, token_number DESC
)
WHERE deleted_at IS NULL AND token_number IS NOT NULL;

-- Index for location-based filtering
-- Use case: Find all home visits for a branch
-- Example: SELECT * FROM appointments WHERE branch_id = ? AND appointment_location = 'HOME'
CREATE INDEX idx_appointments_branch_location ON appointments(branch_id, appointment_location)
WHERE deleted_at IS NULL AND appointment_location != 'CLINIC';

-- Index for patient-branch appointments
-- Use case: Patient's appointments at specific branch
-- Example: SELECT * FROM appointments WHERE patient_id = ? AND branch_id = ? AND status != 'CANCELLED'
CREATE INDEX idx_appointments_patient_branch ON appointments(patient_id, branch_id, appointment_time DESC)
WHERE deleted_at IS NULL;

-- ============================================================================
-- USER TABLE ENHANCEMENT INDEXES
-- ============================================================================

-- Composite index for branch-scoped user queries
-- Use case: Get all active staff at a specific branch
-- Example: SELECT * FROM users WHERE branch_id = ? AND status = 'ACTIVE'
CREATE INDEX idx_users_branch_status ON users(branch_id, status)
WHERE deleted_at IS NULL;

-- ============================================================================
-- PATIENT TABLE ENHANCEMENT INDEXES
-- ============================================================================

-- Index for branch-scoped patient queries
-- Use case: Get all patients with primary branch assignment
-- Example: SELECT * FROM patients WHERE primary_branch_id = ? AND status != 'INACTIVE'
CREATE INDEX idx_patients_branch_created ON patients(primary_branch_id, created_at DESC)
WHERE deleted_at IS NULL;

-- ============================================================================
-- INDEX STATISTICS AND DOCUMENTATION
-- ============================================================================

COMMENT ON INDEX idx_branches_created_by IS 'Audit index: query branches by creator. Useful for staff accountability.';
COMMENT ON INDEX idx_branches_tenant_active IS 'Composite index: optimizes common tenant+active branches queries.';

COMMENT ON INDEX idx_treatments_created_by IS 'Audit index: track treatments created by staff members.';
COMMENT ON INDEX idx_treatments_cost_active IS 'Cost analysis index: filter treatments by price range.';
COMMENT ON INDEX idx_treatments_tenant_category_active IS 'Browsing index: efficient category filtering within tenant.';

COMMENT ON INDEX idx_medical_orders_created_by IS 'Audit index: orders placed by specific staff.';
COMMENT ON INDEX idx_medical_orders_patient_date IS 'Patient history: retrieve all orders for a patient chronologically.';
COMMENT ON INDEX idx_medical_orders_tenant_status IS 'Dashboard index: filter orders by status within tenant.';
COMMENT ON INDEX idx_medical_orders_overdue IS 'Alert index: efficiently find overdue orders for notifications.';
COMMENT ON INDEX idx_medical_orders_delivery_dates IS 'Analytics index: lead time analysis and performance reporting.';

COMMENT ON INDEX idx_appointments_doctor_branch_time_detailed IS 'Scheduling index: doctor schedule at specific branch with status filtering.';
COMMENT ON INDEX idx_appointments_doctor_token_by_date IS 'Token generation index: sequential token assignment per doctor per day (Sequences: monotonic sequences).';
COMMENT ON INDEX idx_appointments_branch_location IS 'Location filtering index: efficiently find home visits or virtual appointments.';
COMMENT ON INDEX idx_appointments_patient_branch IS 'Patient schedule index: appointments at specific branch in chronological order.';

COMMENT ON INDEX idx_users_branch_status IS 'Staff directory index: find active staff at specific branches.';
COMMENT ON INDEX idx_patients_branch_created IS 'Patient roster index: patients registered at specific branch, sorted by creation date.';

-- ============================================================================
-- PERFORMANCE MONITORING QUERIES
-- ============================================================================
-- Run these queries post-migration to verify index effectiveness:

-- 1. Check index usage statistics
-- SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
-- FROM pg_stat_user_indexes
-- WHERE tablename IN ('branches', 'treatments', 'medical_orders', 'appointments', 'users', 'patients')
-- ORDER BY idx_scan DESC;

-- 2. Identify unused indexes
-- SELECT schemaname, tablename, indexname
-- FROM pg_stat_user_indexes
-- WHERE idx_scan = 0
-- AND tablename IN ('branches', 'treatments', 'medical_orders', 'appointments')
-- ORDER BY tablename, indexname;

-- 3. Check index size
-- SELECT
--     schemaname,
--     tablename,
--     indexname,
--     pg_size_pretty(pg_relation_size(indexrelid)) as index_size
-- FROM pg_stat_user_indexes
-- WHERE tablename IN ('branches', 'treatments', 'medical_orders', 'appointments', 'users', 'patients')
-- ORDER BY pg_relation_size(indexrelid) DESC;
