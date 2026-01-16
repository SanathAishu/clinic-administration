-- Phase D Feature 3: Prescription Enhancement with Inventory Integration
-- Version 16: Enhance prescriptions table with state machine and refill management
-- Date: 2025-01-16
--
-- Mathematical Foundation:
-- Theorem 12: Atomic Inventory Deduction
--   When dispensing prescription, inventory reduction is atomic - either both
--   prescription is marked DISPENSED and inventory is reduced, or neither happens.
--   @Transactional in Java guarantees this via database rollback.
--
-- Theorem 11: Quantity Invariant
--   prescribedQuantity = dosage × frequencyPerDay × durationDays
--   dispensedQuantity ≤ prescribedQuantity (cannot dispense more than prescribed)
--
-- State Machine DAG (Directed Acyclic Graph):
-- PENDING ──dispense()──> DISPENSED ──complete()──> COMPLETED
--    │                                                  ▲
--    └──────────cancel()───────────────────────────────┘
--
-- CANCELLED and COMPLETED are terminal states (no outgoing transitions)

-- Step 1: Rename existing status column and add new state machine columns
ALTER TABLE prescriptions
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PENDING' NOT NULL;

-- Step 2: Add state transition timestamps
ALTER TABLE prescriptions
ADD COLUMN IF NOT EXISTS dispensed_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ;

-- Step 3: Add refill management columns
ALTER TABLE prescriptions
ADD COLUMN IF NOT EXISTS allowed_refills INTEGER DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS times_filled INTEGER DEFAULT 0 NOT NULL;

-- Step 4: Add tracking for who dispensed and cancelled
ALTER TABLE prescriptions
ADD COLUMN IF NOT EXISTS dispensed_by UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS cancelled_by UUID REFERENCES users(id);

-- Step 5: Enhance prescription_items table
-- Add inventory reference and dosage fields for quantity calculations
ALTER TABLE prescription_items
ADD COLUMN IF NOT EXISTS inventory_id UUID REFERENCES inventory(id),
ADD COLUMN IF NOT EXISTS dosage NUMERIC(10, 2),
ADD COLUMN IF NOT EXISTS dosage_unit VARCHAR(50),
ADD COLUMN IF NOT EXISTS frequency_per_day INTEGER,
ADD COLUMN IF NOT EXISTS duration_days INTEGER,
ADD COLUMN IF NOT EXISTS prescribed_quantity INTEGER,
ADD COLUMN IF NOT EXISTS dispensed_quantity INTEGER DEFAULT 0;

-- Step 6: Create drug_interactions table
-- Stores known drug-drug interactions with severity levels
CREATE TABLE IF NOT EXISTS drug_interactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    medication_a_id UUID NOT NULL REFERENCES inventory(id) ON DELETE CASCADE,
    medication_b_id UUID NOT NULL REFERENCES inventory(id) ON DELETE CASCADE,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('MINOR', 'MODERATE', 'SEVERE')),
    description TEXT NOT NULL,
    recommendation TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    -- Unique constraint on interaction pair (A, B) per tenant
    -- Ensures no duplicate interactions
    CONSTRAINT uk_drug_interactions_pair UNIQUE (tenant_id, medication_a_id, medication_b_id)
);

-- Step 7: Add check constraints for state consistency
-- Enforces temporal ordering and state-timestamp consistency
ALTER TABLE prescriptions
ADD CONSTRAINT IF NOT EXISTS rx_temporal_order CHECK (
    (dispensed_at IS NULL OR dispensed_at >= created_at) AND
    (completed_at IS NULL OR (dispensed_at IS NOT NULL AND completed_at >= dispensed_at)) AND
    (cancelled_at IS NULL OR cancelled_at >= created_at)
),
ADD CONSTRAINT IF NOT EXISTS rx_refill_constraint CHECK (times_filled <= (allowed_refills + 1)),
ADD CONSTRAINT IF NOT EXISTS rx_times_filled_nonnegative CHECK (times_filled >= 0),
ADD CONSTRAINT IF NOT EXISTS rx_allowed_refills_nonnegative CHECK (allowed_refills >= 0);

-- Step 8: Add check constraints for prescription items
-- Enforces quantity invariant: dispensedQuantity ≤ prescribedQuantity
ALTER TABLE prescription_items
ADD CONSTRAINT IF NOT EXISTS item_quantity_invariant CHECK (dispensed_quantity <= prescribed_quantity),
ADD CONSTRAINT IF NOT EXISTS item_dosage_positive CHECK (dosage > 0),
ADD CONSTRAINT IF NOT EXISTS item_frequency_positive CHECK (frequency_per_day > 0),
ADD CONSTRAINT IF NOT EXISTS item_duration_positive CHECK (duration_days > 0),
ADD CONSTRAINT IF NOT EXISTS item_prescribed_positive CHECK (prescribed_quantity > 0),
ADD CONSTRAINT IF NOT EXISTS item_dispensed_nonnegative CHECK (dispensed_quantity >= 0);

-- Step 9: Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_prescriptions_status ON prescriptions(status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_prescriptions_patient_status ON prescriptions(patient_id, status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_prescriptions_doctor_status ON prescriptions(doctor_id, status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_prescriptions_dispensed_at ON prescriptions(dispensed_at) WHERE deleted_at IS NULL AND dispensed_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_prescription_items_inventory ON prescription_items(inventory_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_drug_interactions_severity ON drug_interactions(severity) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_drug_interactions_medications ON drug_interactions(medication_a_id, medication_b_id) WHERE deleted_at IS NULL;

-- Step 10: Add comments for documentation
COMMENT ON COLUMN prescriptions.status IS 'Prescription state: PENDING, DISPENSED, COMPLETED, CANCELLED';
COMMENT ON COLUMN prescriptions.dispensed_at IS 'Timestamp when prescription was dispensed to patient';
COMMENT ON COLUMN prescriptions.completed_at IS 'Timestamp when patient completed medication course';
COMMENT ON COLUMN prescriptions.cancelled_at IS 'Timestamp when prescription was cancelled';
COMMENT ON COLUMN prescriptions.allowed_refills IS 'Number of refills allowed (total fills = allowed_refills + 1)';
COMMENT ON COLUMN prescriptions.times_filled IS 'Number of times prescription has been filled/dispensed';
COMMENT ON COLUMN prescriptions.dispensed_by IS 'User who dispensed the prescription';
COMMENT ON COLUMN prescriptions.cancelled_by IS 'User who cancelled the prescription';

COMMENT ON COLUMN prescription_items.inventory_id IS 'Reference to inventory/medication item';
COMMENT ON COLUMN prescription_items.dosage IS 'Dosage amount (e.g., 500 for 500mg)';
COMMENT ON COLUMN prescription_items.dosage_unit IS 'Unit of measurement (mg, ml, tablet, capsule)';
COMMENT ON COLUMN prescription_items.frequency_per_day IS 'How many times per day to take medication';
COMMENT ON COLUMN prescription_items.duration_days IS 'Number of days to take medication';
COMMENT ON COLUMN prescription_items.prescribed_quantity IS 'Total quantity prescribed = dosage × frequency × duration';
COMMENT ON COLUMN prescription_items.dispensed_quantity IS 'Actual quantity dispensed to patient';

COMMENT ON TABLE drug_interactions IS 'Known drug-drug interactions with severity levels (MINOR, MODERATE, SEVERE)';
COMMENT ON COLUMN drug_interactions.severity IS 'Interaction severity: MINOR (monitor), MODERATE (adjust dose), SEVERE (avoid)';
COMMENT ON COLUMN drug_interactions.description IS 'Clinical description of the interaction';
COMMENT ON COLUMN drug_interactions.recommendation IS 'Clinical recommendation for managing interaction';
