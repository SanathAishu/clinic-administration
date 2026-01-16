-- ============================================================================
-- V10: Create Branch Entity and Support Enums
-- ============================================================================
-- This migration creates:
-- 1. Enums for appointment location types (CLINIC, HOME, VIRTUAL)
-- 2. Enum for medical order statuses (8-state machine)
-- 3. Branch table for multi-location clinic support
--
-- Discrete Math Principles Applied:
-- - Set Theory: Unique constraints on branch codes per tenant
-- - Invariants: At least one contact method (phone or email) required
-- - Cardinality: Only one main branch per tenant
-- ============================================================================

-- ============================================================================
-- ENUMS
-- ============================================================================

-- Appointment location enum (Feature #5, #6)
-- CLINIC: Patient visits clinic facility
-- HOME: Doctor performs house visit at patient's address
-- VIRTUAL: Telemedicine/online consultation
CREATE TYPE appointment_location_enum AS ENUM ('CLINIC', 'HOME', 'VIRTUAL');

-- Medical order status enum (Feature #12)
-- State machine with 8 states for product order lifecycle
-- PENDING → SENT → IN_PRODUCTION → SHIPPED → RECEIVED → READY_FOR_PICKUP → DELIVERED
-- PENDING → CANCELLED (anytime before DELIVERED)
-- RECEIVED → READY_FOR_PICKUP (item received, ready for patient pickup)
CREATE TYPE order_status_enum AS ENUM (
    'PENDING',
    'SENT',
    'IN_PRODUCTION',
    'SHIPPED',
    'RECEIVED',
    'READY_FOR_PICKUP',
    'DELIVERED',
    'CANCELLED'
);

-- ============================================================================
-- BRANCH TABLE (Feature #13: Multi-branch/Multi-location support)
-- ============================================================================
-- Represents physical clinic locations/branches
-- One tenant can have multiple branches (but only one main branch)
--
-- Discrete Math Constraints:
-- - Uniqueness: branch_code unique per tenant (Set Theory: injective function)
-- - Cardinality: max 1 main branch per tenant (enforced by constraint + service logic)
-- - Invariant: At least one contact method required (phone OR email)
--
-- Operations Research:
-- - Resource capacity constraints: maxPatientsPerDay, maxConcurrentAppointments
-- - Capacity planning: Track branch-level scheduling constraints
-- ============================================================================

CREATE TABLE branches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,

    -- Branch Identification (Uniqueness: code unique per tenant)
    branch_code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,

    -- Location Details
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(10),
    country VARCHAR(100) NOT NULL DEFAULT 'India',

    -- Contact Information (Invariant: at least one required)
    phone VARCHAR(15),
    email VARCHAR(255),

    -- Operational Details
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_main_branch BOOLEAN NOT NULL DEFAULT false,

    -- Operating Schedule
    operating_hours TEXT,

    -- Capacity Constraints (Operations Research: Resource allocation)
    max_patients_per_day INTEGER,
    max_concurrent_appointments INTEGER,

    -- Additional Information
    description TEXT,
    facilities TEXT,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    deleted_at TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT uk_branches_tenant_code UNIQUE (tenant_id, branch_code),
    CONSTRAINT branch_code_format CHECK (branch_code ~ '^[A-Z0-9_-]{2,50}$'),
    CONSTRAINT branch_contact_required CHECK (
        (phone IS NOT NULL AND phone != '') OR
        (email IS NOT NULL AND email != '')
    ),
    CONSTRAINT branch_capacity_positive CHECK (
        max_patients_per_day IS NULL OR max_patients_per_day > 0
    ),
    CONSTRAINT branch_concurrent_positive CHECK (
        max_concurrent_appointments IS NULL OR max_concurrent_appointments > 0
    ),
    CONSTRAINT branch_main_is_active CHECK (
        is_main_branch = false OR is_active = true
    )
);

-- ============================================================================
-- BRANCH INDEXES
-- ============================================================================

-- Tenant query index (RLS + Multi-tenancy)
CREATE INDEX idx_branches_tenant ON branches(tenant_id)
WHERE deleted_at IS NULL;

-- Branch code lookup (feature usage)
CREATE INDEX idx_branches_code ON branches(branch_code)
WHERE deleted_at IS NULL;

-- Active branches query (filtering)
CREATE INDEX idx_branches_active ON branches(is_active)
WHERE deleted_at IS NULL;

-- Main branch lookup (quick access)
CREATE INDEX idx_branches_main ON branches(tenant_id, is_main_branch)
WHERE is_main_branch = true AND deleted_at IS NULL;

-- ============================================================================
-- TABLE COMMENTS
-- ============================================================================

COMMENT ON TABLE branches IS 'Branch/Location table for multi-branch clinic support. One tenant can have multiple branches but only one designated main branch.';

COMMENT ON COLUMN branches.id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN branches.tenant_id IS 'Multi-tenancy: isolates data per organization';
COMMENT ON COLUMN branches.branch_code IS 'Unique code per tenant (e.g., MAIN, NORTH, SOUTH). Format: 2-50 uppercase alphanumeric with underscores/hyphens';
COMMENT ON COLUMN branches.is_main_branch IS 'Designates the primary/headquarters branch (only one per tenant)';
COMMENT ON COLUMN branches.max_patients_per_day IS 'Capacity constraint: Maximum patients that can be scheduled in a day (Operations Research: Resource allocation)';
COMMENT ON COLUMN branches.max_concurrent_appointments IS 'Capacity constraint: Maximum concurrent appointment slots available (Operations Research: Queuing theory)';
COMMENT ON COLUMN branches.deleted_at IS 'Soft delete timestamp (null = active, not null = deleted)';

COMMENT ON CONSTRAINT uk_branches_tenant_code ON branches IS 'Ensures branch codes are unique per tenant (Discrete Math: Set Theory - injective function)';
COMMENT ON CONSTRAINT branch_contact_required ON branches IS 'Invariant: at least one contact method required for communication (Discrete Math: Invariant)';
COMMENT ON CONSTRAINT branch_main_is_active ON branches IS 'Invariant: main branch must be active (Discrete Math: State consistency)';
