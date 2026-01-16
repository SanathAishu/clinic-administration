-- ============================================
-- Migration: V7__add_tenant_id_to_lab_result
-- Description: Add tenant_id column to lab_results for direct multi-tenancy isolation
-- Security Fix: Ensures Row Level Security (RLS) without relying on joins
-- ============================================

-- Add tenant_id column (nullable initially)
ALTER TABLE lab_results
ADD COLUMN tenant_id UUID;

-- Populate tenant_id from parent lab_tests table
UPDATE lab_results lr
SET tenant_id = (
    SELECT lt.tenant_id
    FROM lab_tests lt
    WHERE lt.id = lr.lab_test_id
);

-- Verify all rows have tenant_id populated
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM lab_results WHERE tenant_id IS NULL) THEN
        RAISE EXCEPTION 'Migration failed: Some lab_results records could not be assigned a tenant_id';
    END IF;
END $$;

-- Make tenant_id NOT NULL
ALTER TABLE lab_results
ALTER COLUMN tenant_id SET NOT NULL;

-- Create index for tenant-based queries
CREATE INDEX idx_lab_results_tenant ON lab_results(tenant_id);

-- Add foreign key constraint to tenants table
ALTER TABLE lab_results
ADD CONSTRAINT fk_lab_results_tenant
FOREIGN KEY (tenant_id) REFERENCES tenants(id)
ON DELETE CASCADE;

-- Add comment for documentation
COMMENT ON COLUMN lab_results.tenant_id IS 'Direct tenant isolation field for Row Level Security';
