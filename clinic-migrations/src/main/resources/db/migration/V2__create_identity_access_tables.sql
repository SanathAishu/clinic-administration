-- ============================================================================
-- V2: Create Identity & Access Tables
-- ============================================================================
-- This migration creates tables for session management and permissions:
-- 1. sessions - JWT token tracking and session management
-- 2. permissions - Fine-grained permission system
-- 3. role_permissions - Many-to-many relationship
-- ============================================================================

-- ============================================================================
-- 1. SESSIONS TABLE
-- ============================================================================
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_jti VARCHAR(255) UNIQUE NOT NULL,
    refresh_token_jti VARCHAR(255) UNIQUE,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    refresh_expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    revoked_by UUID REFERENCES users(id),

    CONSTRAINT sessions_expiry_order CHECK (expires_at > created_at),
    CONSTRAINT sessions_refresh_expiry CHECK (refresh_expires_at IS NULL OR refresh_expires_at > expires_at)
);

CREATE INDEX idx_sessions_tenant_user ON sessions(tenant_id, user_id);
CREATE INDEX idx_sessions_token ON sessions(token_jti) WHERE revoked_at IS NULL;
CREATE INDEX idx_sessions_expires ON sessions(expires_at) WHERE revoked_at IS NULL;
CREATE INDEX idx_sessions_user_active ON sessions(user_id, expires_at) WHERE revoked_at IS NULL;

-- ============================================================================
-- 2. PERMISSIONS TABLE
-- ============================================================================
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT permissions_resource_action_unique UNIQUE (resource, action)
);

CREATE INDEX idx_permissions_resource ON permissions(resource);

-- ============================================================================
-- 3. ROLE_PERMISSIONS TABLE (Many-to-Many)
-- ============================================================================
CREATE TABLE role_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    granted_by UUID REFERENCES users(id),

    CONSTRAINT role_permissions_unique UNIQUE (role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);

-- ============================================================================
-- Insert Default Permissions
-- ============================================================================

-- User Management Permissions
INSERT INTO permissions (name, resource, action, description) VALUES
('users.create', 'users', 'create', 'Create new users'),
('users.read', 'users', 'read', 'View user details'),
('users.update', 'users', 'update', 'Update user information'),
('users.delete', 'users', 'delete', 'Soft delete users'),
('users.list', 'users', 'list', 'List all users'),

-- Role Management Permissions
('roles.create', 'roles', 'create', 'Create new roles'),
('roles.read', 'roles', 'read', 'View role details'),
('roles.update', 'roles', 'update', 'Update role information'),
('roles.delete', 'roles', 'delete', 'Soft delete roles'),
('roles.list', 'roles', 'list', 'List all roles'),

-- Patient Management Permissions
('patients.create', 'patients', 'create', 'Register new patients'),
('patients.read', 'patients', 'read', 'View patient details'),
('patients.update', 'patients', 'update', 'Update patient information'),
('patients.delete', 'patients', 'delete', 'Soft delete patients'),
('patients.list', 'patients', 'list', 'List all patients'),
('patients.export', 'patients', 'export', 'Export patient data'),

-- Appointment Management Permissions
('appointments.create', 'appointments', 'create', 'Schedule appointments'),
('appointments.read', 'appointments', 'read', 'View appointment details'),
('appointments.update', 'appointments', 'update', 'Modify appointments'),
('appointments.cancel', 'appointments', 'cancel', 'Cancel appointments'),
('appointments.list', 'appointments', 'list', 'List all appointments'),

-- Medical Records Permissions
('medical_records.create', 'medical_records', 'create', 'Create medical records'),
('medical_records.read', 'medical_records', 'read', 'View medical records'),
('medical_records.update', 'medical_records', 'update', 'Update medical records'),
('medical_records.list', 'medical_records', 'list', 'List medical records'),

-- Prescription Permissions
('prescriptions.create', 'prescriptions', 'create', 'Write prescriptions'),
('prescriptions.read', 'prescriptions', 'read', 'View prescriptions'),
('prescriptions.update', 'prescriptions', 'update', 'Modify prescriptions'),
('prescriptions.list', 'prescriptions', 'list', 'List prescriptions'),

-- Lab Test Permissions
('lab_tests.create', 'lab_tests', 'create', 'Order lab tests'),
('lab_tests.read', 'lab_tests', 'read', 'View lab test orders'),
('lab_tests.update', 'lab_tests', 'update', 'Update lab test orders'),
('lab_tests.list', 'lab_tests', 'list', 'List lab tests'),

-- Lab Results Permissions
('lab_results.create', 'lab_results', 'create', 'Enter lab results'),
('lab_results.read', 'lab_results', 'read', 'View lab results'),
('lab_results.update', 'lab_results', 'update', 'Update lab results'),
('lab_results.list', 'lab_results', 'list', 'List lab results'),

-- Billing Permissions
('billing.create', 'billing', 'create', 'Create bills'),
('billing.read', 'billing', 'read', 'View bills'),
('billing.update', 'billing', 'update', 'Update bills'),
('billing.list', 'billing', 'list', 'List bills'),

-- Inventory Permissions
('inventory.create', 'inventory', 'create', 'Add inventory items'),
('inventory.read', 'inventory', 'read', 'View inventory'),
('inventory.update', 'inventory', 'update', 'Update inventory'),
('inventory.delete', 'inventory', 'delete', 'Remove inventory items'),
('inventory.list', 'inventory', 'list', 'List inventory items'),

-- Reports & Analytics Permissions
('reports.patient', 'reports', 'patient', 'Generate patient reports'),
('reports.financial', 'reports', 'financial', 'Generate financial reports'),
('reports.inventory', 'reports', 'inventory', 'Generate inventory reports'),
('reports.audit', 'reports', 'audit', 'View audit logs'),

-- System Administration Permissions
('system.settings', 'system', 'settings', 'Manage system settings'),
('system.backup', 'system', 'backup', 'Perform system backups'),
('system.logs', 'system', 'logs', 'View system logs');

-- ============================================================================
-- Comments
-- ============================================================================

COMMENT ON TABLE sessions IS 'JWT session tracking for token revocation and audit';
COMMENT ON TABLE permissions IS 'Fine-grained permission system for RBAC';
COMMENT ON TABLE role_permissions IS 'Maps permissions to roles';
