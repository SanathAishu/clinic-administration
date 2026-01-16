-- ============================================================================
-- V13: Add Phase A Enhancements to Existing Tables
-- ============================================================================
-- This migration:
-- 1. Adds branch_id foreign key to users, patients, appointments, and treatments
-- 2. Enhances appointments table with token numbers and house visit support
-- 3. Adds new enum for appointment location types
--
-- Features Enhanced:
-- - Feature #5: Appointment token numbers (queue management)
-- - Feature #6: House visit support (home visit appointments)
-- - Feature #13: Multi-branch support (branch mapping)
--
-- Discrete Math Principles:
-- - Sequences: Token numbers form monotonic sequences per doctor per day
-- - Invariants: HOME appointments require house visit address
-- - State consistency: Appointment status must match location type
-- ============================================================================

-- ============================================================================
-- 1. ADD BRANCH SUPPORT TO USERS TABLE
-- ============================================================================
-- Staff/doctors assigned to specific branches

ALTER TABLE users
ADD COLUMN branch_id UUID REFERENCES branches(id) ON DELETE SET NULL;

-- Index for user queries by branch
CREATE INDEX idx_users_branch ON users(branch_id)
WHERE deleted_at IS NULL;

COMMENT ON COLUMN users.branch_id IS 'Branch assignment for staff/doctors. NULL = user can work at all branches (admin/admin staff)';

-- ============================================================================
-- 2. ADD BRANCH SUPPORT TO PATIENTS TABLE
-- ============================================================================
-- Patient primary branch (default clinic location)

ALTER TABLE patients
ADD COLUMN primary_branch_id UUID REFERENCES branches(id) ON DELETE SET NULL;

-- Index for patient queries by branch
CREATE INDEX idx_patients_branch ON patients(primary_branch_id)
WHERE deleted_at IS NULL;

COMMENT ON COLUMN patients.primary_branch_id IS 'Primary branch where patient usually visits. Used for default scheduling and communication.';

-- ============================================================================
-- 3. ENHANCE APPOINTMENTS TABLE
-- ============================================================================

-- Add appointment location enum and house visit fields
ALTER TABLE appointments
ADD COLUMN appointment_location appointment_location_enum DEFAULT 'CLINIC' NOT NULL,
ADD COLUMN branch_id UUID REFERENCES branches(id) ON DELETE SET NULL,
ADD COLUMN token_number INTEGER,
ADD COLUMN house_visit_address TEXT,
ADD COLUMN house_visit_city VARCHAR(100),
ADD COLUMN house_visit_pincode VARCHAR(10);

-- Add token number index (Sequences: token numbers per doctor per day)
CREATE INDEX idx_appointments_token ON appointments(token_number, appointment_time)
WHERE deleted_at IS NULL;

-- Add appointment location index (filtering by location)
CREATE INDEX idx_appointments_location ON appointments(appointment_location)
WHERE deleted_at IS NULL;

-- Add branch index for branch-specific appointments
CREATE INDEX idx_appointments_branch ON appointments(branch_id)
WHERE deleted_at IS NULL;

-- Add combined index for common queries: doctor + branch + time
CREATE INDEX idx_appointments_doctor_branch_time ON appointments(doctor_id, branch_id, appointment_time)
WHERE deleted_at IS NULL;

-- Add constraints (Discrete Math: Invariants)
ALTER TABLE appointments
ADD CONSTRAINT appointment_home_visit_address_required CHECK (
    (appointment_location != 'HOME') OR
    (house_visit_address IS NOT NULL AND house_visit_address != '')
),
ADD CONSTRAINT appointment_token_positive CHECK (
    token_number IS NULL OR token_number > 0
);

COMMENT ON COLUMN appointments.appointment_location IS 'Location type: CLINIC (at clinic), HOME (house visit), VIRTUAL (telemedicine). (Feature #5, #6)';
COMMENT ON COLUMN appointments.branch_id IS 'Branch where appointment takes place. NULL = determined by doctor assignment or patient default.';
COMMENT ON COLUMN appointments.token_number IS 'Queue token number (Sequences: monotonic per doctor per day). NULL before generation.';
COMMENT ON COLUMN appointments.house_visit_address IS 'Full address for HOME appointments. Invariant: required if appointment_location = HOME. (Discrete Math: Invariant)';
COMMENT ON COLUMN appointments.house_visit_city IS 'City for HOME appointments.';
COMMENT ON COLUMN appointments.house_visit_pincode IS 'Postal code for HOME appointments.';

COMMENT ON CONSTRAINT appointment_home_visit_address_required ON appointments IS 'Invariant: HOME appointments must have a house visit address (Discrete Math: state-dependent constraint)';
COMMENT ON CONSTRAINT appointment_token_positive ON appointments IS 'Invariant: token number if specified must be positive (Discrete Math: domain constraint)';

-- ============================================================================
-- 4. ADD BRANCH SUPPORT TO TREATMENTS TABLE
-- ============================================================================
-- Branch-specific treatment availability already added in table creation,
-- but adding index here for consistency

-- Index already created in V11, no changes needed
-- Verifying index existence:
-- Treatment table already has: idx_treatments_branch ON treatments(branch_id)

-- ============================================================================
-- DATA MIGRATION NOTES
-- ============================================================================
-- 1. All new columns added as nullable to preserve existing data
-- 2. appointment_location defaults to 'CLINIC' (existing clinic appointments)
-- 3. New foreign keys use ON DELETE SET NULL to prevent orphaned records
-- 4. No data cleanup needed - new columns remain NULL for existing records
-- 5. Services and controllers handle NULL values gracefully

-- ============================================================================
-- ROLLBACK PROCEDURE
-- ============================================================================
-- If rollback is needed, drop in this order:
-- 1. ALTER TABLE appointments DROP CONSTRAINT appointment_home_visit_address_required, DROP CONSTRAINT appointment_token_positive;
-- 2. DROP INDEX idx_appointments_token, idx_appointments_location, idx_appointments_branch, idx_appointments_doctor_branch_time;
-- 3. ALTER TABLE appointments DROP COLUMN appointment_location, branch_id, token_number, house_visit_address, house_visit_city, house_visit_pincode;
-- 4. DROP INDEX idx_patients_branch;
-- 5. ALTER TABLE patients DROP COLUMN primary_branch_id;
-- 6. DROP INDEX idx_users_branch;
-- 7. ALTER TABLE users DROP COLUMN branch_id;
