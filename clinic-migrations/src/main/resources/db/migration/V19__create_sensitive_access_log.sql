-- V19: Create Sensitive Data Access Log Table
-- Purpose: Immutable append-only audit trail for sensitive data access
-- ISO 27001 Alignment: A.12.4.1 (Event logging), A.12.4.2 (Protection of log information)

-- Create ENUM type for access types
CREATE TYPE access_type AS ENUM (
    'VIEW_MEDICAL_RECORD',
    'VIEW_PRESCRIPTION',
    'VIEW_LAB_RESULT',
    'VIEW_PATIENT_DETAILS',
    'EXPORT_PATIENT_DATA',
    'MODIFY_MEDICAL_RECORD',
    'PRINT_PRESCRIPTION',
    'VIEW_BILLING_DETAILS'
);

-- Create sensitive_data_access_log table
CREATE TABLE sensitive_data_access_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    user_id UUID NOT NULL REFERENCES users(id),
    patient_id UUID REFERENCES patients(id),

    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    access_type access_type NOT NULL,

    access_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    access_reason TEXT,
    data_exported BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT sdal_immutable CHECK (deleted_at IS NULL)
);

-- Create indexes for audit trail queries
CREATE INDEX idx_sdal_tenant_timestamp ON sensitive_data_access_log(tenant_id, access_timestamp DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_sdal_user ON sensitive_data_access_log(user_id, access_timestamp DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_sdal_entity ON sensitive_data_access_log(entity_type, entity_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_sdal_patient ON sensitive_data_access_log(patient_id, access_timestamp DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_sdal_access_type ON sensitive_data_access_log(access_type, access_timestamp DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_sdal_ip_address ON sensitive_data_access_log(ip_address, access_timestamp DESC) WHERE deleted_at IS NULL;

-- Enforce immutability: No updates allowed to audit logs
CREATE TRIGGER prevent_sensitive_access_log_update
BEFORE UPDATE ON sensitive_data_access_log
FOR EACH ROW
EXECUTE FUNCTION prevent_update();

-- Enforce immutability: No deletes allowed to audit logs (soft deletes only)
CREATE TRIGGER prevent_sensitive_access_log_delete
BEFORE DELETE ON sensitive_data_access_log
FOR EACH ROW
EXECUTE FUNCTION prevent_delete();

-- RLS Policy: Tenants can only see their own access logs
ALTER TABLE sensitive_data_access_log ENABLE ROW LEVEL SECURITY;

CREATE POLICY sensitive_data_access_log_tenant_isolation ON sensitive_data_access_log
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

-- Only admins and compliance officers can view access logs
CREATE POLICY sensitive_data_access_log_admin_only ON sensitive_data_access_log
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM user_roles
            WHERE user_id = current_setting('app.current_user_id')::uuid
            AND (role_id = (SELECT id FROM roles WHERE code = 'ADMIN')
                 OR role_id = (SELECT id FROM roles WHERE code = 'COMPLIANCE_OFFICER'))
        )
    );

GRANT SELECT, INSERT ON sensitive_data_access_log TO clinic_user;

-- Create partition table comment for future monthly partitioning
-- Partitioning can be enabled manually when needed for better performance
COMMENT ON TABLE sensitive_data_access_log IS 'Immutable append-only audit log for sensitive data access. Future: Can be partitioned by month for performance. Example: sensitive_data_access_log_2026_01, sensitive_data_access_log_2026_02, etc.';

-- Helper function to prevent updates
CREATE OR REPLACE FUNCTION prevent_update()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit logs are immutable and cannot be updated';
END;
$$ LANGUAGE plpgsql;

-- Helper function to prevent deletes
CREATE OR REPLACE FUNCTION prevent_delete()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit logs are immutable and cannot be deleted';
END;
$$ LANGUAGE plpgsql;
