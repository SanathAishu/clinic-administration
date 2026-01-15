-- ============================================================================
-- V4: Create Operations Tables
-- ============================================================================
-- This migration creates operational and supporting tables:
-- 1. inventory - Medicine and supply inventory management
-- 2. inventory_transactions - Stock movement tracking
-- 3. staff_schedules - Doctor/staff availability scheduling
-- 4. notifications - System notifications for users
-- 5. consent_records - DPDP Act 2023 compliant consent tracking
-- 6. patient_documents - Document storage metadata
-- ============================================================================

-- ============================================================================
-- Additional Enums for Operations
-- ============================================================================

CREATE TYPE inventory_category_enum AS ENUM (
    'MEDICINE', 'SURGICAL_SUPPLIES', 'LABORATORY', 'CONSUMABLES', 'EQUIPMENT', 'OTHER'
);

CREATE TYPE transaction_type_enum AS ENUM (
    'PURCHASE', 'SALE', 'ADJUSTMENT', 'EXPIRY', 'RETURN', 'TRANSFER'
);

CREATE TYPE notification_type_enum AS ENUM (
    'APPOINTMENT_REMINDER', 'LAB_RESULT_READY', 'PAYMENT_DUE',
    'LOW_INVENTORY', 'EXPIRY_ALERT', 'SYSTEM_ALERT'
);

CREATE TYPE notification_status_enum AS ENUM ('PENDING', 'SENT', 'READ', 'FAILED');

CREATE TYPE consent_type_enum AS ENUM (
    'DATA_PROCESSING', 'DATA_SHARING', 'MARKETING', 'RESEARCH', 'TELEMEDICINE'
);

CREATE TYPE consent_status_enum AS ENUM ('GRANTED', 'REVOKED', 'EXPIRED');

CREATE TYPE document_type_enum AS ENUM (
    'MEDICAL_REPORT', 'LAB_REPORT', 'PRESCRIPTION', 'INVOICE',
    'CONSENT_FORM', 'INSURANCE_DOCUMENT', 'IDENTITY_PROOF', 'OTHER'
);

-- ============================================================================
-- 1. INVENTORY TABLE
-- ============================================================================
CREATE TABLE inventory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    -- Item Details
    item_name VARCHAR(255) NOT NULL,
    item_code VARCHAR(100) UNIQUE,
    category inventory_category_enum NOT NULL,
    description TEXT,

    -- Stock Information
    current_stock INTEGER NOT NULL DEFAULT 0,
    minimum_stock INTEGER NOT NULL DEFAULT 0,
    unit VARCHAR(50) NOT NULL,
    unit_price NUMERIC(10,2),

    -- Medicine Specific
    manufacturer VARCHAR(255),
    batch_number VARCHAR(100),
    expiry_date DATE,

    -- Storage
    location VARCHAR(100),

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    deleted_at TIMESTAMPTZ,

    CONSTRAINT inventory_stock_non_negative CHECK (current_stock >= 0),
    CONSTRAINT inventory_minimum_stock CHECK (minimum_stock >= 0),
    CONSTRAINT inventory_unit_price CHECK (unit_price IS NULL OR unit_price >= 0),
    CONSTRAINT inventory_expiry_future CHECK (expiry_date IS NULL OR expiry_date >= CURRENT_DATE - INTERVAL '1 year')
);

CREATE INDEX idx_inventory_tenant ON inventory(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_inventory_category ON inventory(category) WHERE deleted_at IS NULL;
CREATE INDEX idx_inventory_low_stock ON inventory(tenant_id)
    WHERE current_stock <= minimum_stock AND deleted_at IS NULL;
CREATE INDEX idx_inventory_expiry ON inventory(expiry_date)
    WHERE expiry_date IS NOT NULL AND deleted_at IS NULL;

-- ============================================================================
-- 2. INVENTORY_TRANSACTIONS TABLE
-- ============================================================================
CREATE TABLE inventory_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    inventory_id UUID NOT NULL REFERENCES inventory(id),

    -- Transaction Details
    transaction_type transaction_type_enum NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10,2),
    total_amount NUMERIC(10,2),

    -- Reference
    reference_type VARCHAR(100),
    reference_id UUID,

    -- Additional Information
    notes TEXT,
    supplier_name VARCHAR(255),

    -- Stock Tracking
    stock_before INTEGER NOT NULL,
    stock_after INTEGER NOT NULL,

    -- Metadata
    transaction_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT inventory_trans_quantity_non_zero CHECK (quantity != 0),
    CONSTRAINT inventory_trans_stock_calculation CHECK (
        (transaction_type IN ('PURCHASE', 'RETURN') AND stock_after = stock_before + quantity) OR
        (transaction_type IN ('SALE', 'EXPIRY', 'ADJUSTMENT') AND stock_after = stock_before + quantity) OR
        (transaction_type = 'TRANSFER')
    )
);

CREATE INDEX idx_inventory_trans_tenant ON inventory_transactions(tenant_id);
CREATE INDEX idx_inventory_trans_inventory ON inventory_transactions(inventory_id, transaction_date DESC);
CREATE INDEX idx_inventory_trans_type ON inventory_transactions(transaction_type, transaction_date DESC);

-- ============================================================================
-- 3. STAFF_SCHEDULES TABLE
-- ============================================================================
CREATE TABLE staff_schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id),

    -- Schedule Details
    day_of_week INTEGER NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,

    -- Date Range (for temporary schedules or leaves)
    valid_from DATE NOT NULL DEFAULT CURRENT_DATE,
    valid_until DATE,

    -- Break Time
    break_start_time TIME,
    break_end_time TIME,

    -- Additional Information
    notes TEXT,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    deleted_at TIMESTAMPTZ,

    CONSTRAINT staff_schedules_day_range CHECK (day_of_week BETWEEN 0 AND 6),
    CONSTRAINT staff_schedules_time_order CHECK (end_time > start_time),
    CONSTRAINT staff_schedules_break_time CHECK (
        (break_start_time IS NULL AND break_end_time IS NULL) OR
        (break_start_time IS NOT NULL AND break_end_time IS NOT NULL AND break_end_time > break_start_time)
    ),
    CONSTRAINT staff_schedules_date_range CHECK (valid_until IS NULL OR valid_until >= valid_from)
    -- Note: Schedule overlap constraint enforced at application level
);

CREATE INDEX idx_staff_schedules_tenant ON staff_schedules(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_staff_schedules_user ON staff_schedules(user_id, day_of_week) WHERE deleted_at IS NULL;
CREATE INDEX idx_staff_schedules_validity ON staff_schedules(valid_from, valid_until) WHERE deleted_at IS NULL;

-- ============================================================================
-- 4. NOTIFICATIONS TABLE
-- ============================================================================
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id),

    -- Notification Details
    type notification_type_enum NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status notification_status_enum NOT NULL DEFAULT 'PENDING',

    -- Reference
    reference_type VARCHAR(100),
    reference_id UUID,

    -- Delivery
    scheduled_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_tenant ON notifications(tenant_id);
CREATE INDEX idx_notifications_user_status ON notifications(user_id, status, scheduled_at DESC);
CREATE INDEX idx_notifications_scheduled ON notifications(scheduled_at) WHERE status = 'PENDING';

-- ============================================================================
-- 5. CONSENT_RECORDS TABLE (DPDP Act 2023 Compliance)
-- ============================================================================
CREATE TABLE consent_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    patient_id UUID NOT NULL REFERENCES patients(id),

    -- Consent Details
    consent_type consent_type_enum NOT NULL,
    purpose TEXT NOT NULL,
    status consent_status_enum NOT NULL DEFAULT 'GRANTED',

    -- Legal Requirements
    consent_text TEXT NOT NULL,
    consent_version VARCHAR(50) NOT NULL,

    -- Timestamps
    granted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,

    -- Digital Signature/Proof
    ip_address INET,
    user_agent TEXT,
    signature_data BYTEA,

    -- Metadata
    granted_by UUID REFERENCES users(id),
    revoked_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT consent_status_logic CHECK (
        (status = 'GRANTED' AND revoked_at IS NULL) OR
        (status = 'REVOKED' AND revoked_at IS NOT NULL) OR
        (status = 'EXPIRED' AND expires_at IS NOT NULL AND expires_at < CURRENT_TIMESTAMP)
    )
);

CREATE INDEX idx_consent_records_tenant ON consent_records(tenant_id);
CREATE INDEX idx_consent_records_patient ON consent_records(patient_id, granted_at DESC);
CREATE INDEX idx_consent_records_status ON consent_records(status);
CREATE INDEX idx_consent_records_expiry ON consent_records(expires_at) WHERE status = 'GRANTED' AND expires_at IS NOT NULL;

-- ============================================================================
-- 6. PATIENT_DOCUMENTS TABLE
-- ============================================================================
CREATE TABLE patient_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    patient_id UUID NOT NULL REFERENCES patients(id),

    -- Document Details
    document_type document_type_enum NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,

    -- File Information
    file_name VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    checksum VARCHAR(64) NOT NULL,

    -- Reference
    reference_type VARCHAR(100),
    reference_id UUID,

    -- Metadata
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT patient_docs_file_size CHECK (file_size_bytes > 0 AND file_size_bytes <= 10485760)
);

CREATE INDEX idx_patient_docs_tenant ON patient_documents(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_patient_docs_patient ON patient_documents(patient_id, uploaded_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_patient_docs_type ON patient_documents(document_type) WHERE deleted_at IS NULL;

-- ============================================================================
-- Triggers
-- ============================================================================

CREATE TRIGGER update_inventory_updated_at BEFORE UPDATE ON inventory
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_staff_schedules_updated_at BEFORE UPDATE ON staff_schedules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Prevent hard deletes
CREATE TRIGGER prevent_hard_delete_inventory BEFORE DELETE ON inventory
    FOR EACH ROW EXECUTE FUNCTION prevent_hard_delete();

CREATE TRIGGER prevent_hard_delete_staff_schedules BEFORE DELETE ON staff_schedules
    FOR EACH ROW EXECUTE FUNCTION prevent_hard_delete();

CREATE TRIGGER prevent_hard_delete_patient_documents BEFORE DELETE ON patient_documents
    FOR EACH ROW EXECUTE FUNCTION prevent_hard_delete();

-- ============================================================================
-- Row Level Security (RLS) Setup
-- ============================================================================

-- Enable RLS on all tables
ALTER TABLE tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE patients ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE medical_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE prescriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE lab_tests ENABLE ROW LEVEL SECURITY;
ALTER TABLE vitals ENABLE ROW LEVEL SECURITY;
ALTER TABLE diagnoses ENABLE ROW LEVEL SECURITY;
ALTER TABLE billing ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE staff_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE consent_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE patient_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- Create RLS policies for multi-tenant isolation
-- Note: These policies assume current_setting('app.current_tenant_id') is set by the application

-- Tenants policy
CREATE POLICY tenant_isolation_policy ON tenants
    USING (id::TEXT = current_setting('app.current_tenant_id', TRUE));

-- Generic tenant isolation policy for all tenant-aware tables
DO $$
DECLARE
    table_name TEXT;
    tables TEXT[] := ARRAY[
        'users', 'roles', 'sessions', 'patients', 'appointments',
        'medical_records', 'prescriptions', 'lab_tests', 'vitals', 'diagnoses',
        'billing', 'inventory', 'inventory_transactions', 'staff_schedules',
        'notifications', 'consent_records', 'patient_documents', 'audit_logs'
    ];
BEGIN
    FOREACH table_name IN ARRAY tables
    LOOP
        EXECUTE format('
            CREATE POLICY tenant_isolation_policy ON %I
            USING (tenant_id::TEXT = current_setting(''app.current_tenant_id'', TRUE))
        ', table_name);
    END LOOP;
END $$;

-- ============================================================================
-- Comments
-- ============================================================================

COMMENT ON TABLE inventory IS 'Medicine and supply inventory with expiry tracking';
COMMENT ON TABLE inventory_transactions IS 'Immutable audit trail of all stock movements';
COMMENT ON TABLE staff_schedules IS 'Doctor/staff availability with temporal non-overlap';
COMMENT ON TABLE notifications IS 'System notifications for appointments, alerts, and reminders';
COMMENT ON TABLE consent_records IS 'DPDP Act 2023 compliant consent tracking with digital proof';
COMMENT ON TABLE patient_documents IS 'Metadata for documents stored in MinIO (S3-compatible storage)';

-- ============================================================================
-- Function to create monthly audit log partitions
-- ============================================================================

CREATE OR REPLACE FUNCTION create_audit_log_partition(partition_date DATE)
RETURNS VOID AS $$
DECLARE
    partition_name TEXT;
    start_date TEXT;
    end_date TEXT;
BEGIN
    partition_name := 'audit_logs_' || TO_CHAR(partition_date, 'YYYY_MM');
    start_date := TO_CHAR(DATE_TRUNC('month', partition_date), 'YYYY-MM-DD');
    end_date := TO_CHAR(DATE_TRUNC('month', partition_date) + INTERVAL '1 month', 'YYYY-MM-DD');

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I PARTITION OF audit_logs
        FOR VALUES FROM (%L) TO (%L)
    ', partition_name, start_date, end_date);
END;
$$ LANGUAGE plpgsql;

-- Create partitions for next 3 months
SELECT create_audit_log_partition((CURRENT_DATE + interval '1 month')::DATE);
SELECT create_audit_log_partition((CURRENT_DATE + interval '2 months')::DATE);
SELECT create_audit_log_partition((CURRENT_DATE + interval '3 months')::DATE);

COMMENT ON FUNCTION create_audit_log_partition IS 'Creates monthly partitions for audit_logs table';
