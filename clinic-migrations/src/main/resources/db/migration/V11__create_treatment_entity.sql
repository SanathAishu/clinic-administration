-- ============================================================================
-- V11: Create Treatment/Service Catalog Entity
-- ============================================================================
-- This migration creates:
-- 1. Treatment table for service/treatment catalog
--
-- Feature #11: Treatment/Service Catalog
-- Stores all available treatments and services with pricing and discounts
--
-- Discrete Math Principles:
-- - Invariants: Discount percentage must be 0-100%, base cost non-negative
-- - Financial calculations: Final cost = base_cost * (1 - discount_percentage/100)
--
-- Operations Research:
-- - Cost optimization: Track base cost, discount percentage, final cost
-- - Service management: Category-based organization for resource planning
-- ============================================================================

CREATE TABLE treatments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID REFERENCES branches(id) ON DELETE SET NULL,

    -- Treatment/Service Details
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),

    -- Pricing (Operations Research: Cost optimization)
    base_cost NUMERIC(10, 2) NOT NULL,
    discount_percentage NUMERIC(5, 2) DEFAULT 0.00,
    -- Final cost calculated as: base_cost * (1 - discount_percentage/100)
    -- This is computed in application layer and stored for reporting

    -- Service Duration
    duration_minutes INTEGER,

    -- Additional Information
    instructions TEXT,
    prerequisites TEXT,

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    deleted_at TIMESTAMPTZ,

    -- Constraints (Discrete Math: Invariants)
    CONSTRAINT treatment_base_cost_positive CHECK (base_cost >= 0),
    CONSTRAINT treatment_discount_range CHECK (
        discount_percentage >= 0 AND discount_percentage <= 100
    ),
    CONSTRAINT treatment_duration_positive CHECK (
        duration_minutes IS NULL OR duration_minutes > 0
    )
);

-- ============================================================================
-- TREATMENT INDEXES
-- ============================================================================

-- Tenant query index (RLS + Multi-tenancy)
CREATE INDEX idx_treatments_tenant ON treatments(tenant_id)
WHERE deleted_at IS NULL;

-- Branch query index (branch-specific treatments)
CREATE INDEX idx_treatments_branch ON treatments(branch_id)
WHERE deleted_at IS NULL;

-- Name search index (feature: search by name)
CREATE INDEX idx_treatments_name ON treatments(name)
WHERE deleted_at IS NULL;

-- Category lookup (feature: filter by category)
CREATE INDEX idx_treatments_category ON treatments(category)
WHERE deleted_at IS NULL;

-- Active treatments query (filtering)
CREATE INDEX idx_treatments_active ON treatments(is_active)
WHERE deleted_at IS NULL;

-- Combined index for common queries: tenant + active + category
CREATE INDEX idx_treatments_tenant_active_category ON treatments(tenant_id, is_active, category)
WHERE deleted_at IS NULL;

-- ============================================================================
-- TABLE COMMENTS
-- ============================================================================

COMMENT ON TABLE treatments IS 'Treatment/Service catalog. Stores all available medical treatments and services with pricing and discount information.';

COMMENT ON COLUMN treatments.id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN treatments.tenant_id IS 'Multi-tenancy: isolates catalog per organization';
COMMENT ON COLUMN treatments.branch_id IS 'Optional: branch-specific treatments. NULL = available at all branches';
COMMENT ON COLUMN treatments.base_cost IS 'Base price before any discounts. Must be non-negative (Discrete Math: Invariant)';
COMMENT ON COLUMN treatments.discount_percentage IS 'Discount percentage (0-100). Final cost = base_cost * (1 - discount_percentage/100) (Discrete Math: Invariant)';
COMMENT ON COLUMN treatments.duration_minutes IS 'Estimated duration for the treatment/service in minutes';
COMMENT ON COLUMN treatments.is_active IS 'Soft activation flag. Active treatments available for booking';
COMMENT ON COLUMN treatments.deleted_at IS 'Soft delete timestamp (null = active, not null = deleted)';

COMMENT ON CONSTRAINT treatment_base_cost_positive ON treatments IS 'Invariant: base cost must be non-negative (Discrete Math: domain constraint)';
COMMENT ON CONSTRAINT treatment_discount_range ON treatments IS 'Invariant: discount percentage must be between 0 and 100 inclusive (Discrete Math: range constraint)';
COMMENT ON CONSTRAINT treatment_duration_positive ON treatments IS 'Invariant: duration if specified must be positive (Discrete Math: domain constraint)';
