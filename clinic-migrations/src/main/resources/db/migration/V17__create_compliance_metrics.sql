-- V17: Create Compliance Metrics Table
-- Purpose: Store statistical process control metrics for SLA monitoring
-- ISO 27001 Alignment: A.18 (Compliance)

-- Create ENUM type for metric types
CREATE TYPE compliance_metric_type AS ENUM (
    'QUEUE_STABILITY',
    'WAIT_TIME_SLA',
    'CACHE_HIT_RATE',
    'ERROR_RATE',
    'ACCESS_LOG_COVERAGE',
    'DATA_RETENTION_COMPLIANCE',
    'CONSENT_VALIDITY'
);

-- Create compliance_metrics table
CREATE TABLE compliance_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    metric_date DATE NOT NULL,
    metric_type compliance_metric_type NOT NULL,

    -- SLA Metrics
    total_transactions BIGINT,
    sla_violations BIGINT,
    compliance_rate NUMERIC(5, 2) CHECK (compliance_rate IS NULL OR (compliance_rate >= 0 AND compliance_rate <= 100)),

    -- Statistical Control
    mean_value NUMERIC(10, 4),
    std_deviation NUMERIC(10, 4) CHECK (std_deviation IS NULL OR std_deviation >= 0),
    ucl NUMERIC(10, 4),
    lcl NUMERIC(10, 4),
    out_of_control BOOLEAN DEFAULT FALSE,

    -- ISO 27001 Specific
    access_violations BIGINT DEFAULT 0 CHECK (access_violations >= 0),
    data_integrity_errors BIGINT DEFAULT 0 CHECK (data_integrity_errors >= 0),
    security_incidents BIGINT DEFAULT 0 CHECK (security_incidents >= 0),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT compliance_metrics_unique_date_type UNIQUE (tenant_id, metric_date, metric_type)
);

-- Create indexes for query optimization
CREATE INDEX idx_compliance_metrics_tenant ON compliance_metrics(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_compliance_metrics_date ON compliance_metrics(metric_date DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_compliance_metrics_type ON compliance_metrics(metric_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_compliance_metrics_out_of_control ON compliance_metrics(tenant_id)
    WHERE out_of_control = TRUE AND deleted_at IS NULL;

-- Create trigger for automatic updated_at timestamp
CREATE TRIGGER compliance_metrics_updated_at
BEFORE UPDATE ON compliance_metrics
FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

-- RLS Policy: Tenants can only see their own metrics
ALTER TABLE compliance_metrics ENABLE ROW LEVEL SECURITY;

CREATE POLICY compliance_metrics_tenant_isolation ON compliance_metrics
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

GRANT SELECT, INSERT, UPDATE ON compliance_metrics TO clinic_user;
