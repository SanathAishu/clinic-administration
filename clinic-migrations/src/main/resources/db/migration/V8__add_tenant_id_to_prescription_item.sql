-- ============================================
-- Migration: V8__add_tenant_id_to_prescription_item
-- Description: Add tenant_id column to prescription_items for direct multi-tenancy isolation
-- Security Fix: Ensures Row Level Security (RLS) without relying on joins
-- ============================================

-- Add tenant_id column (nullable initially)
ALTER TABLE prescription_items
ADD COLUMN tenant_id UUID;

-- Populate tenant_id from parent prescriptions table
UPDATE prescription_items pi
SET tenant_id = (
    SELECT p.tenant_id
    FROM prescriptions p
    WHERE p.id = pi.prescription_id
);

-- Verify all rows have tenant_id populated
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM prescription_items WHERE tenant_id IS NULL) THEN
        RAISE EXCEPTION 'Migration failed: Some prescription_items records could not be assigned a tenant_id';
    END IF;
END $$;

-- Make tenant_id NOT NULL
ALTER TABLE prescription_items
ALTER COLUMN tenant_id SET NOT NULL;

-- Create index for tenant-based queries
CREATE INDEX idx_prescription_items_tenant ON prescription_items(tenant_id);

-- Add foreign key constraint to tenants table
ALTER TABLE prescription_items
ADD CONSTRAINT fk_prescription_items_tenant
FOREIGN KEY (tenant_id) REFERENCES tenants(id)
ON DELETE CASCADE;

-- Add comment for documentation
COMMENT ON COLUMN prescription_items.tenant_id IS 'Direct tenant isolation field for Row Level Security';
