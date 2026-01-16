-- V18: Create Data Retention and Archival Tables
-- Purpose: Manage data lifecycle with automated archival and deletion
-- ISO 27001 Alignment: A.18.1.3 (Protection of Records), A.18.1.4 (Privacy and PII)

-- Create ENUM types
CREATE TYPE entity_type AS ENUM (
    'AUDIT_LOG',
    'PATIENT_RECORD',
    'MEDICAL_RECORD',
    'BILLING_RECORD',
    'APPOINTMENT',
    'PRESCRIPTION',
    'CONSENT_RECORD',
    'SESSION',
    'NOTIFICATION'
);

CREATE TYPE archival_action AS ENUM (
    'SOFT_DELETE',
    'EXPORT_TO_S3',
    'ANONYMIZE',
    'HARD_DELETE'
);

-- Create data_retention_policies table
CREATE TABLE data_retention_policies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    entity_type entity_type NOT NULL,
    retention_days INTEGER NOT NULL CHECK (retention_days >= 1),
    grace_period_days INTEGER NOT NULL DEFAULT 30 CHECK (grace_period_days >= 0),
    archival_action archival_action NOT NULL,

    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_execution TIMESTAMPTZ,
    records_archived BIGINT DEFAULT 0 CHECK (records_archived >= 0),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT retention_policy_unique_entity UNIQUE (tenant_id, entity_type)
);

-- Create indexes for policy queries
CREATE INDEX idx_retention_policies_tenant ON data_retention_policies(tenant_id);
CREATE INDEX idx_retention_policies_enabled ON data_retention_policies(tenant_id)
    WHERE enabled = TRUE;

-- Create trigger for automatic updated_at timestamp
CREATE TRIGGER data_retention_policies_updated_at
BEFORE UPDATE ON data_retention_policies
FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

-- RLS Policy: Tenants can only manage their own policies
ALTER TABLE data_retention_policies ENABLE ROW LEVEL SECURITY;

CREATE POLICY data_retention_policies_tenant_isolation ON data_retention_policies
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

GRANT SELECT, INSERT, UPDATE ON data_retention_policies TO clinic_user;

-- Create data_archival_log table for execution tracking
CREATE TABLE data_archival_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    policy_id UUID NOT NULL REFERENCES data_retention_policies(id),

    execution_date DATE NOT NULL,
    entity_type entity_type NOT NULL,
    records_processed BIGINT DEFAULT 0 CHECK (records_processed >= 0),
    records_archived BIGINT DEFAULT 0 CHECK (records_archived >= 0),
    records_failed BIGINT DEFAULT 0 CHECK (records_failed >= 0),

    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    duration_seconds INTEGER CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    error_message TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for archival log queries
CREATE INDEX idx_archival_log_tenant ON data_archival_log(tenant_id);
CREATE INDEX idx_archival_log_execution_date ON data_archival_log(execution_date DESC);
CREATE INDEX idx_archival_log_status ON data_archival_log(status)
    WHERE status = 'RUNNING' OR status = 'FAILED';
CREATE INDEX idx_archival_log_policy ON data_archival_log(policy_id);

-- RLS Policy: Tenants can only see their own archival logs
ALTER TABLE data_archival_log ENABLE ROW LEVEL SECURITY;

CREATE POLICY data_archival_log_tenant_isolation ON data_archival_log
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

GRANT SELECT, INSERT ON data_archival_log TO clinic_user;

-- Default retention policies for common entity types
-- These can be overridden per tenant
INSERT INTO data_retention_policies (tenant_id, entity_type, retention_days, grace_period_days, archival_action)
SELECT id, 'AUDIT_LOG'::entity_type, 2555, 30, 'EXPORT_TO_S3'::archival_action
FROM tenants
ON CONFLICT (tenant_id, entity_type) DO NOTHING;

INSERT INTO data_retention_policies (tenant_id, entity_type, retention_days, grace_period_days, archival_action)
SELECT id, 'PATIENT_RECORD'::entity_type, 2555, 30, 'EXPORT_TO_S3'::archival_action
FROM tenants
ON CONFLICT (tenant_id, entity_type) DO NOTHING;

INSERT INTO data_retention_policies (tenant_id, entity_type, retention_days, grace_period_days, archival_action)
SELECT id, 'SESSION'::entity_type, 90, 7, 'HARD_DELETE'::archival_action
FROM tenants
ON CONFLICT (tenant_id, entity_type) DO NOTHING;

INSERT INTO data_retention_policies (tenant_id, entity_type, retention_days, grace_period_days, archival_action)
SELECT id, 'NOTIFICATION'::entity_type, 30, 0, 'HARD_DELETE'::archival_action
FROM tenants
ON CONFLICT (tenant_id, entity_type) DO NOTHING;
