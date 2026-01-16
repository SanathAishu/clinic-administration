# Clinic Management System - Complete Project Specification

## Table of Contents
1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Compliance Requirements](#compliance-requirements)
4. [Architecture & Design Principles](#architecture--design-principles)
5. [Database Schema](#database-schema)
6. [Project Structure](#project-structure)
7. [Implementation Patterns](#implementation-patterns)
8. [VM Deployment Infrastructure](#vm-deployment-infrastructure)
9. [Development Roadmap](#development-roadmap)

---

## Project Overview

### Description
Multi-tenant clinic management system designed for physiotherapy clinics, general practices, and specialty clinics in India. This is a **staff-facing system only** - patients do not have login access. All patient data is recorded and managed by clinic staff on behalf of patients.

### Key Features
- Multi-tenant architecture with row-level security
- Comprehensive patient management (demographics, medical records, consents)
- Appointment scheduling with conflict prevention
- Clinical documentation (encounters, diagnoses, prescriptions)
- Laboratory order management
- Document storage and management
- Audit logging with compliance tracking
- ABHA ID integration (Ayushman Bharat Health Account)

### Target Users
- Clinic Administrators
- Doctors/Physicians
- Nurses
- Receptionists
- Lab Technicians
- Pharmacists

---

## Technology Stack

### Backend Stack

| Component | Technology | Version | License |
|-----------|-----------|---------|---------|
| **Runtime** | Eclipse Temurin JDK | 21 LTS | GPL v2 + Classpath Exception |
| **Framework** | Spring Boot | 3.3.x | Apache 2.0 |
| **Build Tool** | Gradle | 8.x (Kotlin DSL) | Apache 2.0 |
| **ORM** | Hibernate | 6.4+ | LGPL 2.1 |
| **Data Access** | Spring Data JPA | 3.3.x | Apache 2.0 |
| **Query Builder** | QueryDSL | 5.x | Apache 2.0 |
| **Migrations** | Flyway | 10.x | Apache 2.0 |
| **Security** | Spring Security | 6.x | Apache 2.0 |
| **JWT** | jjwt (Java JWT) | 0.12.x | Apache 2.0 |
| **API Docs** | SpringDoc OpenAPI | 2.x | Apache 2.0 |
| **Mapping** | MapStruct | 1.5.x | Apache 2.0 |
| **Utilities** | Lombok | 1.18.x | MIT |
| **Validation** | Hibernate Validator | 8.x | Apache 2.0 |
| **Caching** | Spring Cache + Redis | - | Apache 2.0 |
| **Testing** | JUnit 5, Mockito, TestContainers | - | EPL 2.0, MIT |

### Frontend Stack

| Component | Technology | Version | License |
|-----------|-----------|---------|---------|
| **Framework** | React | 18.x | MIT |
| **Language** | TypeScript | 5.x | Apache 2.0 |
| **Build Tool** | Vite | 5.x | MIT |
| **State (Server)** | TanStack Query (React Query) | 5.x | MIT |
| **State (Client)** | Zustand | 4.x | MIT |
| **Routing** | React Router | 6.x | MIT |
| **Forms** | React Hook Form | 7.x | MIT |
| **Validation** | Zod | 3.x | MIT |
| **UI Components** | shadcn/ui + Radix UI | - | MIT |
| **Styling** | Tailwind CSS | 3.x | MIT |
| **Tables** | TanStack Table | 8.x | MIT |
| **HTTP Client** | Axios | 1.x | MIT |
| **Date/Time** | date-fns | 3.x | MIT |

### Database

| Component | Technology | Version |
|-----------|-----------|---------|
| **Database** | PostgreSQL | 16.x |
| **Extensions** | uuid-ossp, pgcrypto, btree_gist | - |
| **Features** | Row Level Security, Exclusion Constraints, Table Partitioning, Triggers, Custom Domains, ENUMs | - |

### Infrastructure (VM-based Deployment)

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Application Server** | Docker + Docker Compose | Container orchestration |
| **Database** | PostgreSQL 16 (Docker) | Primary database |
| **Cache** | Redis 7 (Docker) | Session cache and data caching |
| **File Storage** | MinIO (Docker) | S3-compatible object storage for documents |
| **Message Queue** | RabbitMQ (Docker) | Async messaging (notifications, audit events) |
| **Reverse Proxy** | Nginx | SSL/TLS termination, load balancing, static files |
| **Monitoring** | Prometheus + Grafana (Docker) | Metrics collection and visualization |
| **Logging** | ELK Stack (Elasticsearch, Logstash, Kibana) | Centralized logging and analysis |
| **Backup** | pg_dump + cron | Automated database backups |
| **SSL/TLS** | Let's Encrypt (Certbot) | Free SSL certificates |

### Development Tools

| Tool | Purpose |
|------|---------|
| **IDE** | Eclipse IDE with Spring Tools 4 (STS) OR VS Code with Java Extension Pack |
| **API Testing** | Bruno (open source Postman alternative) |
| **DB Client** | DBeaver Community Edition |
| **Version Control** | Git + GitHub/GitLab |
| **Container** | Docker + Docker Compose |

---

## Compliance Requirements

### Indian Healthcare Regulations

#### 1. Digital Personal Data Protection Act (DPDP Act) 2023

**Key Requirements:**
- **Consent Management**: Explicit consent required for data processing
  - Implemented via `patient_consents` table
  - Tracks consent type, status, given/withdrawn dates
  - Required consent types: TREATMENT, DATA_PROCESSING, COMMUNICATION

- **Data Breach Notification**: 72-hour notification window
  - Audit logging captures all data access
  - Automated alerts for suspicious activity

- **Data Retention**: Must define and enforce retention periods
  - Configurable per data type
  - Automated purging of expired data (soft delete only)

- **Right to Erasure**: Patients can request data deletion
  - Implemented via soft delete (is_deleted flag)
  - Hard delete prevention via triggers
  - Audit trail maintained even after deletion

- **Data Localization**: Health data must be stored in India
  - VM hosted in India (data localization compliance)
  - No cross-border data transfer

#### 2. Information Technology Act 2000 + SPDI Rules

**Requirements:**
- Health data classified as Sensitive Personal Data or Information (SPDI)
- Mandatory encryption at rest and in transit
  - Database: TDE (Transparent Data Encryption)
  - Transit: TLS 1.3
- Access controls and audit trails
  - Role-based access control (RBAC)
  - All access logged to `audit_logs` table

#### 3. ABDM (Ayushman Bharat Digital Mission) Guidelines

**Requirements:**
- ABHA ID Integration
  - 14-digit unique health identifier
  - Custom domain with regex validation: `^[0-9]{14}$`
  - Optional field in `patients` table

- Health Information Exchange (HIE)
  - APIs for consent-based data sharing
  - FHIR-compliant data formats (future roadmap)

#### 4. Clinical Establishments Act

**Requirements:**
- **Minimum 7-year record retention**
  - Enforced via application logic
  - Configurable retention policies
  - Automated alerts before purging

- **Audit trail for all clinical activities**
  - `audit_logs` table with monthly partitioning
  - Tracks CREATE, READ, UPDATE, DELETE, EXPORT, PRINT operations

### Compliance Implementation Summary

| Requirement | Implementation |
|-------------|----------------|
| Consent Tracking | `patient_consents` table with type, status, timestamps |
| Audit Logging | Partitioned `audit_logs` table with AOP-based aspect |
| Data Retention | Configurable policies + soft delete only |
| Encryption | TLS 1.3 (transit) + TDE (rest) |
| Access Control | RBAC with `roles`, `permissions`, `user_roles` |
| Breach Detection | Audit log monitoring + automated alerts |
| Data Localization | VM hosted in India |
| ABHA Integration | 14-digit ID with domain validation |
| Record Retention | 7-year minimum enforced |

---

## Architecture & Design Principles

### 1. Multi-Tenancy Strategy

**Implementation**: Row Level Security (RLS) at database level

**Benefits:**
- Strong isolation between tenants
- No application-layer tenant filtering required
- Defense-in-depth security

**Mechanism:**
```sql
-- Set session variable from application
SET app.tenant_id = '<tenant_uuid>';

-- RLS policy automatically filters all queries
CREATE POLICY tenant_isolation ON patients
    USING (tenant_id::text = current_setting('app.tenant_id', TRUE));
```

**Application Integration:**
```java
// TenantContext - ThreadLocal storage
public class TenantContext {
    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}

// TenantInterceptor - Sets PostgreSQL session variable
stmt.execute("SET app.tenant_id = '" + tenantId + "'");
```

### 2. Soft Delete Only (Hard Delete Prevention)

**Rationale:**
- Regulatory compliance (7-year retention)
- Audit trail preservation
- Data recovery capability
- Legal protection

**Implementation:**
```sql
-- Trigger prevents hard deletes
CREATE OR REPLACE FUNCTION prevent_hard_delete() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Hard deletes not permitted on table %. Use soft delete.', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

-- Applied to all PHI tables
CREATE TRIGGER trg_patients_no_delete
    BEFORE DELETE ON patients
    FOR EACH ROW
    EXECUTE FUNCTION prevent_hard_delete();
```

**Soft Delete Pattern:**
- `is_deleted` boolean flag (default FALSE)
- `deleted_at` timestamp (nullable)
- `deleted_by` foreign key to users
- Constraint: `is_deleted = TRUE` implies `deleted_at IS NOT NULL`

### 3. Temporal Non-Overlap Constraints

**Use Cases:**
1. User cannot have same role at same branch with overlapping validity periods
2. Provider cannot have overlapping appointments
3. Resources cannot be double-booked

**Implementation:**
```sql
-- User role validity constraint
EXCLUDE USING gist (
    user_id WITH =,
    role_id WITH =,
    branch_id WITH =,
    tstzrange(valid_from, COALESCE(valid_until, 'infinity'::timestamptz), '[)') WITH &&
) WHERE (is_deleted = FALSE);

-- Appointment non-overlap constraint
EXCLUDE USING gist (
    provider_id WITH =,
    tstzrange(scheduled_start, scheduled_end, '[)') WITH &&
) WHERE (is_deleted = FALSE AND cancelled_at IS NULL);
```

### 4. State Machine Enforcement

**Workflow Management:**
- Appointments: SCHEDULED → CONFIRMED → CHECKED_IN → IN_PROGRESS → COMPLETED/CANCELLED/NO_SHOW
- Encounters: IN_PROGRESS → COMPLETED → SIGNED → AMENDED
- Lab Orders: ORDERED → COLLECTED → PROCESSING → COMPLETED/CANCELLED

**Implementation:**
```sql
-- State machine configuration
CREATE TABLE state_machines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(50) NOT NULL UNIQUE,  -- 'appointments', 'encounters', etc.
    name VARCHAR(100) NOT NULL
);

CREATE TABLE state_transitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    state_machine_id UUID NOT NULL REFERENCES state_machines(id),
    from_state VARCHAR(50) NOT NULL,
    to_state VARCHAR(50) NOT NULL,
    is_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(state_machine_id, from_state, to_state)
);

-- Trigger validates state transitions
CREATE OR REPLACE FUNCTION validate_state_transition() RETURNS TRIGGER AS $$
DECLARE
    v_machine_id UUID;
    v_allowed BOOLEAN;
BEGIN
    -- Get state machine ID for this entity type
    SELECT id INTO v_machine_id
    FROM state_machines
    WHERE entity_type = TG_TABLE_NAME;

    -- Check if transition is allowed
    SELECT is_allowed INTO v_allowed
    FROM state_transitions
    WHERE state_machine_id = v_machine_id
      AND from_state = OLD.status::text
      AND to_state = NEW.status::text;

    IF NOT FOUND OR NOT v_allowed THEN
        RAISE EXCEPTION 'Invalid state transition: % -> % for %',
            OLD.status, NEW.status, TG_TABLE_NAME;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

### 5. Discrete Mathematics Principles Applied

| Principle | Implementation | Example |
|-----------|----------------|---------|
| **Set Theory - Uniqueness** | UNIQUE constraints | Natural keys like MRN, email |
| **Set Theory - Disjoint Sets** | ENUM types | Mutually exclusive categories |
| **Relational - Referential Integrity** | Foreign keys | All relationships enforced |
| **Relational - Functional Dependency** | Primary keys | PK determines all attributes |
| **Cardinality - 1:1** | Unique partial index | `encounters.appointment_id` |
| **Cardinality - 1:N** | Foreign keys | One patient, many appointments |
| **Cardinality - M:N** | Junction tables | `user_roles`, `role_permissions` |
| **Temporal Logic - Interval Validity** | CHECK constraints | `valid_from <= valid_until` |
| **Temporal Logic - Non-Overlap** | EXCLUDE constraints | Appointment conflicts |
| **Automata - State Machine** | State transitions table | Workflow validation |
| **Graph Theory - Tenant Consistency** | Trigger validation | Cross-entity tenant matching |
| **Boolean Algebra - Mutual Exclusion** | CHECK constraints | `is_deleted` ⇒ ¬`is_active` |
| **Boolean Algebra - Implication** | CHECK constraints | `cancelled_at` ⇒ `cancelled_by` |
| **Order Theory** | Unique sort_order | Lookup values ordering |
| **Domain Constraints** | Custom domains | Regex validation (email, ABHA, GSTIN) |

---

## Database Schema

### Overview
- **23 Tables** organized in 4 functional groups
- **5 Custom Domains** for format validation
- **9 ENUM Types** for categorical data
- **17 Triggers** for data integrity and audit
- **Partitioned audit_logs** for performance
- **Row Level Security** on all tenant-scoped tables

### Custom Domains (Format Validation)

```sql
-- Email validation
CREATE DOMAIN email_address AS VARCHAR(255)
CHECK (VALUE ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- Medical Record Number (alphanumeric with hyphens)
CREATE DOMAIN mrn_code AS VARCHAR(50)
CHECK (VALUE ~ '^[A-Z0-9-]+$');

-- ABHA ID (14 digits)
CREATE DOMAIN abha_id AS VARCHAR(50)
CHECK (VALUE ~ '^[0-9]{14}$');

-- GSTIN (Goods and Services Tax Identification Number)
CREATE DOMAIN gstin AS VARCHAR(15)
CHECK (VALUE ~ '^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$');

-- PAN (Permanent Account Number)
CREATE DOMAIN pan AS VARCHAR(10)
CHECK (VALUE ~ '^[A-Z]{5}[0-9]{4}[A-Z]{1}$');
```

### ENUM Types

```sql
CREATE TYPE gender AS ENUM ('MALE', 'FEMALE', 'OTHER', 'UNDISCLOSED');

CREATE TYPE blood_group AS ENUM (
    'A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-', 'UNKNOWN'
);

CREATE TYPE consent_type AS ENUM (
    'TREATMENT',        -- Consent for medical treatment
    'DATA_PROCESSING',  -- Consent for data processing (DPDP Act)
    'COMMUNICATION'     -- Consent for marketing/communication
);

CREATE TYPE diagnosis_type AS ENUM (
    'PRIMARY',      -- Main diagnosis
    'SECONDARY',    -- Secondary diagnosis
    'DIFFERENTIAL', -- Differential diagnosis (under consideration)
    'RULE_OUT'      -- Diagnosis to rule out
);

CREATE TYPE lab_priority AS ENUM ('STAT', 'URGENT', 'ROUTINE');

CREATE TYPE appointment_source AS ENUM (
    'WALK_IN', 'ONLINE', 'PHONE', 'REFERRAL'
);

CREATE TYPE medication_route AS ENUM (
    'ORAL', 'TOPICAL', 'IV', 'IM', 'SC', 'INHALED',
    'RECTAL', 'OPHTHALMIC', 'OTIC', 'NASAL'
);

CREATE TYPE notification_channel AS ENUM ('SMS', 'WHATSAPP');

CREATE TYPE audit_action AS ENUM (
    'CREATE', 'READ', 'UPDATE', 'DELETE', 'EXPORT', 'PRINT'
);
```

### Table Groups

#### Foundation Tables (7)
1. **tenants** - Multi-tenant root entity
2. **branches** - Clinic locations per tenant
3. **lookup_types** - Configurable reference data types
4. **lookup_values** - Reference data values
5. **state_machines** - Workflow definition entities
6. **state_machine_states** - Valid states per workflow
7. **state_transitions** - Allowed state transitions

#### Identity & Access Tables (5)
1. **users** - System users (staff only)
2. **roles** - User roles (Doctor, Nurse, Admin, etc.)
3. **permissions** - Granular permissions
4. **role_permissions** - M:N junction table
5. **user_roles** - M:N junction with temporal validity

#### Patient Care Tables (11)
1. **patients** - Patient demographics
2. **patient_consents** - Consent tracking (DPDP compliance)
3. **appointments** - Appointment scheduling
4. **encounters** - Clinical encounters/visits
5. **vital_signs** - Vital signs records
6. **diagnoses** - Diagnosis records
7. **prescriptions** - Prescription headers
8. **prescription_items** - Individual medications
9. **lab_orders** - Lab order headers
10. **lab_order_items** - Individual lab tests
11. **documents** - Document metadata (files in MinIO object storage)

#### Operations Tables (2)
1. **appointment_reminders** - Notification tracking
2. **audit_logs** - Audit trail (partitioned by month)

### Key Schema Features

#### 1. Row Level Security (RLS)

```sql
-- Enable RLS on tenant-scoped tables
ALTER TABLE patients ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only see data for their tenant
CREATE POLICY tenant_isolation ON patients
    USING (tenant_id::text = current_setting('app.tenant_id', TRUE));

-- Applied to all tables: users, patients, appointments, encounters, etc.
```

#### 2. Soft Delete Pattern

```sql
-- Every table has these columns
is_deleted BOOLEAN NOT NULL DEFAULT FALSE
deleted_at TIMESTAMPTZ
deleted_by UUID REFERENCES users(id)

-- Constraints ensure consistency
CONSTRAINT chk_deleted_at CHECK (
    (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL) OR
    (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
)

-- Trigger prevents hard deletes
CREATE TRIGGER trg_<table>_no_delete
    BEFORE DELETE ON <table>
    FOR EACH ROW
    EXECUTE FUNCTION prevent_hard_delete();
```

#### 3. Audit Columns

```sql
-- Every table has these columns
created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
created_by UUID NOT NULL REFERENCES users(id)
updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
updated_by UUID NOT NULL REFERENCES users(id)

-- Trigger auto-updates updated_at
CREATE TRIGGER trg_<table>_updated_at
    BEFORE UPDATE ON <table>
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

#### 4. Temporal Validity (User Roles)

```sql
-- User can have same role at different times
CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    branch_id UUID NOT NULL REFERENCES branches(id),
    valid_from TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMPTZ,  -- NULL means active indefinitely

    -- Prevent overlapping role assignments
    EXCLUDE USING gist (
        user_id WITH =,
        role_id WITH =,
        branch_id WITH =,
        tstzrange(valid_from, COALESCE(valid_until, 'infinity'::timestamptz), '[)') WITH &&
    ) WHERE (is_deleted = FALSE),

    -- Validity constraint
    CONSTRAINT chk_valid_from_until CHECK (
        valid_until IS NULL OR valid_from < valid_until
    )
);
```

#### 5. Appointment Non-Overlap

```sql
CREATE TABLE appointments (
    -- ... other columns ...
    provider_id UUID NOT NULL REFERENCES users(id),
    scheduled_start TIMESTAMPTZ NOT NULL,
    scheduled_end TIMESTAMPTZ NOT NULL,
    cancelled_at TIMESTAMPTZ,

    -- Prevent double-booking
    EXCLUDE USING gist (
        provider_id WITH =,
        tstzrange(scheduled_start, scheduled_end, '[)') WITH &&
    ) WHERE (is_deleted = FALSE AND cancelled_at IS NULL),

    -- Timing constraints
    CONSTRAINT chk_scheduled_times CHECK (scheduled_start < scheduled_end),
    CONSTRAINT chk_duration_reasonable CHECK (
        scheduled_end - scheduled_start <= INTERVAL '8 hours'
    )
);
```

#### 6. One-to-One Relationship (Appointment ↔ Encounter)

```sql
-- An encounter may be linked to one appointment (optional)
-- An appointment can have at most one encounter
CREATE UNIQUE INDEX idx_encounters_appointment_unique
    ON encounters(appointment_id)
    WHERE appointment_id IS NOT NULL AND is_deleted = FALSE;
```

#### 7. Audit Log Partitioning

```sql
-- Parent table (partitioned by month)
CREATE TABLE audit_logs (
    id UUID NOT NULL DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    action audit_action NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    performed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    before_state JSONB,
    after_state JSONB,
    PRIMARY KEY (id, performed_at)
) PARTITION BY RANGE (performed_at);

-- Create monthly partitions
CREATE TABLE audit_logs_2025_01 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE audit_logs_2025_02 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
-- ... etc.
```

#### 8. Boolean Consistency Constraints

```sql
-- Mutual exclusion: deleted implies inactive
CONSTRAINT chk_deleted_inactive CHECK (
    NOT (is_deleted = TRUE AND is_active = TRUE)
)

-- Implication: cancelled_at implies cancelled_by
CONSTRAINT chk_cancelled_by CHECK (
    (cancelled_at IS NULL AND cancelled_by IS NULL) OR
    (cancelled_at IS NOT NULL AND cancelled_by IS NOT NULL)
)

-- Deleted implies deleted_at and deleted_by
CONSTRAINT chk_deleted_at CHECK (
    (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL) OR
    (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
)
```

#### 9. Cross-Entity Tenant Validation

```sql
-- Trigger ensures all entities in a relationship belong to same tenant
CREATE OR REPLACE FUNCTION validate_tenant_consistency() RETURNS TRIGGER AS $$
DECLARE
    v_tenant_id UUID;
BEGIN
    -- Example: When creating an appointment, ensure patient and provider
    -- belong to the same tenant

    SELECT tenant_id INTO v_tenant_id FROM patients WHERE id = NEW.patient_id;

    IF v_tenant_id != NEW.tenant_id THEN
        RAISE EXCEPTION 'Tenant mismatch: patient belongs to different tenant';
    END IF;

    -- Check provider tenant
    SELECT tenant_id INTO v_tenant_id FROM users WHERE id = NEW.provider_id;

    IF v_tenant_id != NEW.tenant_id THEN
        RAISE EXCEPTION 'Tenant mismatch: provider belongs to different tenant';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_appointments_tenant_consistency
    BEFORE INSERT OR UPDATE ON appointments
    FOR EACH ROW
    EXECUTE FUNCTION validate_tenant_consistency();
```

### Indexes

```sql
-- Tenant + Active records (most common filter)
CREATE INDEX idx_patients_tenant_active ON patients(tenant_id)
    WHERE is_deleted = FALSE;

-- Foreign key indexes
CREATE INDEX idx_patients_tenant ON patients(tenant_id);
CREATE INDEX idx_appointments_patient ON appointments(patient_id);
CREATE INDEX idx_appointments_provider ON appointments(provider_id);
CREATE INDEX idx_encounters_patient ON encounters(patient_id);

-- Composite indexes for common queries
CREATE INDEX idx_appointments_date_range ON appointments(scheduled_start, scheduled_end)
    WHERE is_deleted = FALSE AND cancelled_at IS NULL;

-- Partial indexes for active records
CREATE INDEX idx_users_active_email ON users(email)
    WHERE is_deleted = FALSE AND is_active = TRUE;

-- GIN indexes for JSONB columns
CREATE INDEX idx_audit_logs_before_state ON audit_logs USING gin(before_state);
CREATE INDEX idx_audit_logs_after_state ON audit_logs USING gin(after_state);

-- Full-text search indexes
CREATE INDEX idx_patients_search ON patients USING gin(
    to_tsvector('english',
        COALESCE(first_name, '') || ' ' ||
        COALESCE(last_name, '') || ' ' ||
        COALESCE(mrn, '')
    )
);
```

### Sample Data Seeds

#### Lookup Types & Values

```sql
-- Specialties
INSERT INTO lookup_types (tenant_id, code, name) VALUES
    (NULL, 'SPECIALTY', 'Medical Specialties');  -- NULL = system-wide

INSERT INTO lookup_values (lookup_type_id, code, display_name, sort_order) VALUES
    ('<specialty_type_id>', 'GENERAL', 'General Practice', 1),
    ('<specialty_type_id>', 'PHYSIO', 'Physiotherapy', 2),
    ('<specialty_type_id>', 'ORTHO', 'Orthopedics', 3),
    ('<specialty_type_id>', 'CARDIO', 'Cardiology', 4);

-- Appointment Types
INSERT INTO lookup_types (tenant_id, code, name) VALUES
    (NULL, 'APPOINTMENT_TYPE', 'Appointment Types');

INSERT INTO lookup_values (lookup_type_id, code, display_name, sort_order) VALUES
    ('<appt_type_id>', 'CONSULTATION', 'Consultation', 1),
    ('<appt_type_id>', 'FOLLOWUP', 'Follow-up', 2),
    ('<appt_type_id>', 'PROCEDURE', 'Procedure', 3),
    ('<appt_type_id>', 'EMERGENCY', 'Emergency', 4);
```

#### Permissions

```sql
INSERT INTO permissions (resource, action, description) VALUES
    -- Patient Management
    ('PATIENT', 'CREATE', 'Create new patients'),
    ('PATIENT', 'READ', 'View patient information'),
    ('PATIENT', 'UPDATE', 'Update patient information'),
    ('PATIENT', 'DELETE', 'Soft delete patients'),
    ('PATIENT', 'EXPORT', 'Export patient data'),

    -- Appointment Management
    ('APPOINTMENT', 'CREATE', 'Schedule appointments'),
    ('APPOINTMENT', 'READ', 'View appointments'),
    ('APPOINTMENT', 'UPDATE', 'Modify appointments'),
    ('APPOINTMENT', 'CANCEL', 'Cancel appointments'),

    -- Clinical Documentation
    ('ENCOUNTER', 'CREATE', 'Create clinical encounters'),
    ('ENCOUNTER', 'READ', 'View encounters'),
    ('ENCOUNTER', 'UPDATE', 'Update encounters'),
    ('ENCOUNTER', 'SIGN', 'Sign/finalize encounters'),

    -- Prescriptions
    ('PRESCRIPTION', 'CREATE', 'Write prescriptions'),
    ('PRESCRIPTION', 'READ', 'View prescriptions'),
    ('PRESCRIPTION', 'UPDATE', 'Modify prescriptions'),
    ('PRESCRIPTION', 'DELETE', 'Cancel prescriptions'),

    -- Lab Orders
    ('LAB_ORDER', 'CREATE', 'Order lab tests'),
    ('LAB_ORDER', 'READ', 'View lab orders'),
    ('LAB_ORDER', 'UPDATE', 'Update lab orders'),
    ('LAB_ORDER', 'RESULT_ENTRY', 'Enter lab results'),

    -- User Management
    ('USER', 'CREATE', 'Create users'),
    ('USER', 'READ', 'View users'),
    ('USER', 'UPDATE', 'Update users'),
    ('USER', 'DELETE', 'Deactivate users'),
    ('USER', 'ASSIGN_ROLE', 'Assign roles to users'),

    -- Reports & Analytics
    ('REPORT', 'VIEW', 'View reports'),
    ('REPORT', 'EXPORT', 'Export reports'),

    -- System Administration
    ('TENANT', 'MANAGE', 'Manage tenant settings'),
    ('BRANCH', 'MANAGE', 'Manage branches'),
    ('AUDIT_LOG', 'VIEW', 'View audit logs');
```

#### Roles

```sql
INSERT INTO roles (tenant_id, code, name, description) VALUES
    -- System roles (NULL tenant = available to all)
    (NULL, 'SUPER_ADMIN', 'Super Administrator', 'Full system access'),
    (NULL, 'DOCTOR', 'Doctor', 'Clinical provider'),
    (NULL, 'NURSE', 'Nurse', 'Nursing staff'),
    (NULL, 'RECEPTIONIST', 'Receptionist', 'Front desk staff'),
    (NULL, 'LAB_TECH', 'Lab Technician', 'Laboratory staff'),
    (NULL, 'PHARMACIST', 'Pharmacist', 'Pharmacy staff'),
    (NULL, 'ADMIN', 'Administrator', 'Clinic administrator');
```

#### State Machines

```sql
-- Appointment workflow
INSERT INTO state_machines (entity_type, name) VALUES
    ('appointments', 'Appointment Workflow');

INSERT INTO state_machine_states (state_machine_id, state_code, state_name, is_initial, is_final) VALUES
    ('<appt_sm_id>', 'SCHEDULED', 'Scheduled', TRUE, FALSE),
    ('<appt_sm_id>', 'CONFIRMED', 'Confirmed', FALSE, FALSE),
    ('<appt_sm_id>', 'CHECKED_IN', 'Checked In', FALSE, FALSE),
    ('<appt_sm_id>', 'IN_PROGRESS', 'In Progress', FALSE, FALSE),
    ('<appt_sm_id>', 'COMPLETED', 'Completed', FALSE, TRUE),
    ('<appt_sm_id>', 'CANCELLED', 'Cancelled', FALSE, TRUE),
    ('<appt_sm_id>', 'NO_SHOW', 'No Show', FALSE, TRUE);

INSERT INTO state_transitions (state_machine_id, from_state, to_state) VALUES
    ('<appt_sm_id>', 'SCHEDULED', 'CONFIRMED'),
    ('<appt_sm_id>', 'SCHEDULED', 'CANCELLED'),
    ('<appt_sm_id>', 'CONFIRMED', 'CHECKED_IN'),
    ('<appt_sm_id>', 'CONFIRMED', 'CANCELLED'),
    ('<appt_sm_id>', 'CONFIRMED', 'NO_SHOW'),
    ('<appt_sm_id>', 'CHECKED_IN', 'IN_PROGRESS'),
    ('<appt_sm_id>', 'IN_PROGRESS', 'COMPLETED');

-- Encounter workflow
INSERT INTO state_machines (entity_type, name) VALUES
    ('encounters', 'Encounter Workflow');

INSERT INTO state_machine_states (state_machine_id, state_code, state_name, is_initial, is_final) VALUES
    ('<enc_sm_id>', 'IN_PROGRESS', 'In Progress', TRUE, FALSE),
    ('<enc_sm_id>', 'COMPLETED', 'Completed', FALSE, FALSE),
    ('<enc_sm_id>', 'SIGNED', 'Signed', FALSE, TRUE),
    ('<enc_sm_id>', 'AMENDED', 'Amended', FALSE, TRUE);

INSERT INTO state_transitions (state_machine_id, from_state, to_state) VALUES
    ('<enc_sm_id>', 'IN_PROGRESS', 'COMPLETED'),
    ('<enc_sm_id>', 'COMPLETED', 'SIGNED'),
    ('<enc_sm_id>', 'SIGNED', 'AMENDED');

-- Lab Order workflow
INSERT INTO state_machines (entity_type, name) VALUES
    ('lab_orders', 'Lab Order Workflow');

INSERT INTO state_machine_states (state_machine_id, state_code, state_name, is_initial, is_final) VALUES
    ('<lab_sm_id>', 'ORDERED', 'Ordered', TRUE, FALSE),
    ('<lab_sm_id>', 'COLLECTED', 'Sample Collected', FALSE, FALSE),
    ('<lab_sm_id>', 'PROCESSING', 'Processing', FALSE, FALSE),
    ('<lab_sm_id>', 'COMPLETED', 'Completed', FALSE, TRUE),
    ('<lab_sm_id>', 'CANCELLED', 'Cancelled', FALSE, TRUE);

INSERT INTO state_transitions (state_machine_id, from_state, to_state) VALUES
    ('<lab_sm_id>', 'ORDERED', 'COLLECTED'),
    ('<lab_sm_id>', 'ORDERED', 'CANCELLED'),
    ('<lab_sm_id>', 'COLLECTED', 'PROCESSING'),
    ('<lab_sm_id>', 'PROCESSING', 'COMPLETED');
```

---

## Project Structure

### Backend Structure

```
clinic-api/
├── src/
│   ├── main/
│   │   ├── java/com/clinic/
│   │   │   ├── ClinicApplication.java
│   │   │   │
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JpaConfig.java
│   │   │   │   ├── CacheConfig.java
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   └── GcpConfig.java
│   │   │   │
│   │   │   ├── common/
│   │   │   │   ├── audit/
│   │   │   │   │   ├── Audited.java              (Annotation)
│   │   │   │   │   ├── AuditAspect.java          (AOP aspect)
│   │   │   │   │   └── AuditLog.java             (Entity)
│   │   │   │   │
│   │   │   │   ├── exception/
│   │   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   │   ├── InvalidStateTransitionException.java
│   │   │   │   │   ├── TenantMismatchException.java
│   │   │   │   │   └── ValidationException.java
│   │   │   │   │
│   │   │   │   ├── security/
│   │   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   │   ├── SecurityContext.java
│   │   │   │   │   └── UserPrincipal.java
│   │   │   │   │
│   │   │   │   └── tenant/
│   │   │   │       ├── TenantContext.java
│   │   │   │       ├── TenantInterceptor.java
│   │   │   │       └── TenantResolver.java
│   │   │   │
│   │   │   ├── domain/
│   │   │   │   ├── base/
│   │   │   │   │   ├── BaseEntity.java
│   │   │   │   │   └── TenantAwareEntity.java
│   │   │   │   │
│   │   │   │   ├── tenant/
│   │   │   │   │   ├── Tenant.java
│   │   │   │   │   └── Branch.java
│   │   │   │   │
│   │   │   │   ├── user/
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Role.java
│   │   │   │   │   ├── Permission.java
│   │   │   │   │   ├── RolePermission.java
│   │   │   │   │   └── UserRole.java
│   │   │   │   │
│   │   │   │   ├── patient/
│   │   │   │   │   ├── Patient.java
│   │   │   │   │   └── PatientConsent.java
│   │   │   │   │
│   │   │   │   ├── scheduling/
│   │   │   │   │   └── Appointment.java
│   │   │   │   │
│   │   │   │   ├── clinical/
│   │   │   │   │   ├── Encounter.java
│   │   │   │   │   ├── VitalSign.java
│   │   │   │   │   ├── Diagnosis.java
│   │   │   │   │   ├── Prescription.java
│   │   │   │   │   └── PrescriptionItem.java
│   │   │   │   │
│   │   │   │   ├── lab/
│   │   │   │   │   ├── LabOrder.java
│   │   │   │   │   └── LabOrderItem.java
│   │   │   │   │
│   │   │   │   ├── document/
│   │   │   │   │   └── Document.java
│   │   │   │   │
│   │   │   │   └── lookup/
│   │   │   │       ├── LookupType.java
│   │   │   │       └── LookupValue.java
│   │   │   │
│   │   │   ├── repository/
│   │   │   │   ├── TenantRepository.java
│   │   │   │   ├── BranchRepository.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── RoleRepository.java
│   │   │   │   ├── PermissionRepository.java
│   │   │   │   ├── PatientRepository.java
│   │   │   │   ├── AppointmentRepository.java
│   │   │   │   ├── EncounterRepository.java
│   │   │   │   ├── PrescriptionRepository.java
│   │   │   │   ├── LabOrderRepository.java
│   │   │   │   ├── DocumentRepository.java
│   │   │   │   └── LookupValueRepository.java
│   │   │   │
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── UserService.java
│   │   │   │   ├── PatientService.java
│   │   │   │   ├── AppointmentService.java
│   │   │   │   ├── AppointmentStateMachine.java
│   │   │   │   ├── EncounterService.java
│   │   │   │   ├── EncounterStateMachine.java
│   │   │   │   ├── PrescriptionService.java
│   │   │   │   ├── LabOrderService.java
│   │   │   │   ├── LabOrderStateMachine.java
│   │   │   │   ├── DocumentService.java
│   │   │   │   ├── NotificationService.java
│   │   │   │   └── LookupService.java
│   │   │   │
│   │   │   └── api/v1/
│   │   │       ├── controller/
│   │   │       │   ├── AuthController.java
│   │   │       │   ├── UserController.java
│   │   │       │   ├── PatientController.java
│   │   │       │   ├── AppointmentController.java
│   │   │       │   ├── EncounterController.java
│   │   │       │   ├── PrescriptionController.java
│   │   │       │   ├── LabOrderController.java
│   │   │       │   ├── DocumentController.java
│   │   │       │   └── LookupController.java
│   │   │       │
│   │   │       ├── dto/
│   │   │       │   ├── request/
│   │   │       │   │   ├── LoginRequest.java
│   │   │       │   │   ├── CreatePatientRequest.java
│   │   │       │   │   ├── UpdatePatientRequest.java
│   │   │       │   │   ├── CreateAppointmentRequest.java
│   │   │       │   │   └── ...
│   │   │       │   │
│   │   │       │   └── response/
│   │   │       │       ├── LoginResponse.java
│   │   │       │       ├── PatientResponse.java
│   │   │       │       ├── AppointmentResponse.java
│   │   │       │       └── ...
│   │   │       │
│   │   │       └── mapper/
│   │   │           ├── PatientMapper.java
│   │   │           ├── AppointmentMapper.java
│   │   │           ├── EncounterMapper.java
│   │   │           └── ...
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       │
│   │       └── db/migration/
│   │           ├── V1__create_extensions.sql
│   │           ├── V2__create_domains.sql
│   │           ├── V3__create_enums.sql
│   │           ├── V4__create_foundation_tables.sql
│   │           ├── V5__create_identity_tables.sql
│   │           ├── V6__create_patient_tables.sql
│   │           ├── V7__create_clinical_tables.sql
│   │           ├── V8__create_operations_tables.sql
│   │           ├── V9__create_indexes.sql
│   │           ├── V10__create_rls_policies.sql
│   │           ├── V11__create_triggers.sql
│   │           └── V12__seed_data.sql
│   │
│   └── test/
│       └── java/com/clinic/
│           ├── integration/
│           │   ├── PatientControllerTest.java
│           │   ├── AppointmentControllerTest.java
│           │   └── ...
│           │
│           └── unit/
│               ├── service/
│               │   ├── PatientServiceTest.java
│               │   ├── AppointmentServiceTest.java
│               │   └── ...
│               │
│               └── repository/
│                   ├── PatientRepositoryTest.java
│                   └── ...
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── Dockerfile
├── .dockerignore
├── cloudbuild.yaml
└── README.md
```

### Frontend Structure

```
clinic-web/
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── vite-env.d.ts
│   │
│   ├── api/
│   │   ├── axios.ts                    # Axios instance with interceptors
│   │   ├── types.ts                    # API types
│   │   │
│   │   ├── hooks/
│   │   │   ├── usePatients.ts          # TanStack Query hooks
│   │   │   ├── useAppointments.ts
│   │   │   ├── useEncounters.ts
│   │   │   ├── usePrescriptions.ts
│   │   │   ├── useLabOrders.ts
│   │   │   └── useAuth.ts
│   │   │
│   │   └── endpoints/
│   │       ├── patients.ts             # API endpoint functions
│   │       ├── appointments.ts
│   │       ├── encounters.ts
│   │       ├── prescriptions.ts
│   │       ├── labOrders.ts
│   │       └── auth.ts
│   │
│   ├── components/
│   │   ├── ui/                         # shadcn/ui components
│   │   │   ├── button.tsx
│   │   │   ├── input.tsx
│   │   │   ├── select.tsx
│   │   │   ├── dialog.tsx
│   │   │   ├── card.tsx
│   │   │   ├── table.tsx
│   │   │   └── ...
│   │   │
│   │   ├── forms/
│   │   │   ├── PatientForm.tsx
│   │   │   ├── AppointmentForm.tsx
│   │   │   ├── EncounterForm.tsx
│   │   │   ├── PrescriptionForm.tsx
│   │   │   └── LabOrderForm.tsx
│   │   │
│   │   ├── tables/
│   │   │   ├── DataTable.tsx           # Generic data table
│   │   │   ├── PatientTable.tsx
│   │   │   ├── AppointmentTable.tsx
│   │   │   └── ...
│   │   │
│   │   └── layout/
│   │       ├── MainLayout.tsx
│   │       ├── Sidebar.tsx
│   │       ├── Header.tsx
│   │       └── Footer.tsx
│   │
│   ├── features/
│   │   ├── auth/
│   │   │   ├── LoginPage.tsx
│   │   │   ├── ProtectedRoute.tsx
│   │   │   └── AuthGuard.tsx
│   │   │
│   │   ├── patients/
│   │   │   ├── PatientsPage.tsx
│   │   │   ├── PatientDetailPage.tsx
│   │   │   ├── CreatePatientPage.tsx
│   │   │   └── EditPatientPage.tsx
│   │   │
│   │   ├── appointments/
│   │   │   ├── AppointmentsPage.tsx
│   │   │   ├── CalendarView.tsx
│   │   │   ├── AppointmentDetailPage.tsx
│   │   │   └── CreateAppointmentPage.tsx
│   │   │
│   │   ├── encounters/
│   │   │   ├── EncountersPage.tsx
│   │   │   ├── EncounterDetailPage.tsx
│   │   │   └── CreateEncounterPage.tsx
│   │   │
│   │   ├── prescriptions/
│   │   │   ├── PrescriptionsPage.tsx
│   │   │   ├── PrescriptionDetailPage.tsx
│   │   │   └── CreatePrescriptionPage.tsx
│   │   │
│   │   ├── lab-orders/
│   │   │   ├── LabOrdersPage.tsx
│   │   │   ├── LabOrderDetailPage.tsx
│   │   │   └── CreateLabOrderPage.tsx
│   │   │
│   │   ├── dashboard/
│   │   │   └── DashboardPage.tsx
│   │   │
│   │   └── settings/
│   │       ├── SettingsPage.tsx
│   │       ├── UserManagementPage.tsx
│   │       └── BranchManagementPage.tsx
│   │
│   ├── hooks/
│   │   ├── useAuth.ts
│   │   ├── useTenant.ts
│   │   ├── useDebounce.ts
│   │   └── usePermissions.ts
│   │
│   ├── lib/
│   │   ├── utils.ts                    # Utility functions
│   │   ├── constants.ts
│   │   └── validators.ts
│   │
│   ├── stores/
│   │   ├── authStore.ts                # Zustand store
│   │   ├── tenantStore.ts
│   │   └── uiStore.ts
│   │
│   ├── types/
│   │   ├── patient.ts
│   │   ├── appointment.ts
│   │   ├── encounter.ts
│   │   ├── prescription.ts
│   │   ├── labOrder.ts
│   │   └── common.ts
│   │
│   └── styles/
│       └── globals.css
│
├── public/
│   ├── favicon.ico
│   └── logo.svg
│
├── index.html
├── package.json
├── package-lock.json
├── tsconfig.json
├── tsconfig.node.json
├── vite.config.ts
├── tailwind.config.ts
├── postcss.config.js
├── .env.development
├── .env.production
├── Dockerfile
├── nginx.conf
├── .dockerignore
├── cloudbuild.yaml
└── README.md
```

---

## Implementation Patterns

### 1. Multi-Tenancy Implementation

#### TenantContext (ThreadLocal Storage)

```java
package com.clinic.common.tenant;

import java.util.UUID;

public class TenantContext {
    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
```

#### TenantInterceptor (Set PostgreSQL Session Variable)

```java
package com.clinic.common.tenant;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

@Component
public class TenantInterceptor implements StatementInspector {

    @Override
    public String inspect(String sql) {
        UUID tenantId = TenantContext.getTenantId();

        if (tenantId != null) {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SET app.tenant_id = '" + tenantId + "'");
            } catch (SQLException e) {
                throw new RuntimeException("Failed to set tenant context", e);
            }
        }

        return sql;
    }
}
```

#### TenantResolver (Extract from JWT)

```java
package com.clinic.common.tenant;

import com.clinic.common.security.JwtTokenProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Component
public class TenantResolver implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    public TenantResolver(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) {
        String token = jwtTokenProvider.resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            UUID tenantId = jwtTokenProvider.getTenantId(token);
            TenantContext.setTenantId(tenantId);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                               HttpServletResponse response,
                               Object handler,
                               Exception ex) {
        TenantContext.clear();
    }
}
```

### 2. Audit Logging with AOP

#### @Audited Annotation

```java
package com.clinic.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    String action();  // CREATE, UPDATE, DELETE, etc.
    String entityType();
}
```

#### AuditAspect

```java
package com.clinic.common.audit;

import com.clinic.common.security.SecurityContext;
import com.clinic.common.tenant.TenantContext;
import com.clinic.domain.audit.AuditLog;
import com.clinic.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    public AuditAspect(AuditLogRepository auditLogRepository,
                       ObjectMapper objectMapper,
                       HttpServletRequest request) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.request = request;
    }

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Object beforeState = args.length > 0 ? args[0] : null;

        Object result = joinPoint.proceed();

        Object afterState = result;

        AuditLog auditLog = new AuditLog();
        auditLog.setTenantId(TenantContext.getTenantId());
        auditLog.setUserId(SecurityContext.getCurrentUserId());
        auditLog.setAction(audited.action());
        auditLog.setEntityType(audited.entityType());
        auditLog.setEntityId(extractEntityId(result));
        auditLog.setIpAddress(request.getRemoteAddr());
        auditLog.setUserAgent(request.getHeader("User-Agent"));
        auditLog.setBeforeState(objectMapper.writeValueAsString(beforeState));
        auditLog.setAfterState(objectMapper.writeValueAsString(afterState));

        auditLogRepository.save(auditLog);

        return result;
    }

    private UUID extractEntityId(Object entity) {
        // Reflection to get ID field
        // ...
    }
}
```

#### Usage Example

```java
@Service
public class PatientService {

    @Audited(action = "CREATE", entityType = "patients")
    public Patient createPatient(CreatePatientRequest request) {
        // ... create patient logic
    }

    @Audited(action = "UPDATE", entityType = "patients")
    public Patient updatePatient(UUID id, UpdatePatientRequest request) {
        // ... update patient logic
    }
}
```

### 3. State Machine Validation

#### StateMachine Service

```java
package com.clinic.service;

import com.clinic.repository.StateTransitionRepository;
import org.springframework.stereotype.Service;

@Service
public class AppointmentStateMachine {

    private final StateTransitionRepository transitionRepo;

    public AppointmentStateMachine(StateTransitionRepository transitionRepo) {
        this.transitionRepo = transitionRepo;
    }

    public void validateTransition(String currentStatus, String newStatus) {
        boolean isValid = transitionRepo.existsByEntityTypeAndFromStateAndToState(
            "appointments", currentStatus, newStatus);

        if (!isValid) {
            throw new InvalidStateTransitionException(
                "Invalid transition: " + currentStatus + " -> " + newStatus);
        }
    }
}
```

#### Service Layer Usage

```java
@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final AppointmentStateMachine stateMachine;

    public Appointment updateStatus(UUID id, String newStatus) {
        Appointment appointment = appointmentRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // Validate state transition
        stateMachine.validateTransition(
            appointment.getStatus(),
            newStatus
        );

        appointment.setStatus(newStatus);
        return appointmentRepo.save(appointment);
    }
}
```

### 4. Base Entity Classes

#### BaseEntity

```java
package com.clinic.domain.base;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
```

#### TenantAwareEntity

```java
package com.clinic.domain.base;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
}
```

### 5. Repository Pattern with QueryDSL

```java
package com.clinic.repository;

import com.clinic.domain.patient.Patient;
import com.clinic.domain.patient.QPatient;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends
        JpaRepository<Patient, UUID>,
        QuerydslPredicateExecutor<Patient> {

    // Find active patients
    default List<Patient> findActivePatients(UUID tenantId) {
        QPatient patient = QPatient.patient;
        return (List<Patient>) findAll(
            patient.tenantId.eq(tenantId)
                .and(patient.isDeleted.isFalse())
        );
    }

    // Search by name or MRN
    default Page<Patient> searchPatients(UUID tenantId, String searchTerm, Pageable pageable) {
        QPatient patient = QPatient.patient;
        BooleanExpression predicate = patient.tenantId.eq(tenantId)
            .and(patient.isDeleted.isFalse())
            .and(
                patient.firstName.containsIgnoreCase(searchTerm)
                    .or(patient.lastName.containsIgnoreCase(searchTerm))
                    .or(patient.mrn.containsIgnoreCase(searchTerm))
            );
        return findAll(predicate, pageable);
    }

    // Find by MRN
    Optional<Patient> findByTenantIdAndMrnAndIsDeletedFalse(UUID tenantId, String mrn);
}
```

---

## VM Deployment Infrastructure

### Docker Compose Configuration

**docker-compose.yml:**
```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:16-alpine
    container_name: clinic-postgres
    environment:
      POSTGRES_DB: clinic
      POSTGRES_USER: clinic_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      PGDATA: /var/lib/postgresql/data/pgdata
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backups:/backups
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U clinic_user -d clinic"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  # Redis Cache
  redis:
    image: redis:7-alpine
    container_name: clinic-redis
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5
    restart: unless-stopped

  # MinIO (S3-compatible storage)
  minio:
    image: minio/minio:latest
    container_name: clinic-minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    volumes:
      - minio_data:/data
    ports:
      - "9000:9000"
      - "9001:9001"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 30s
      timeout: 20s
      retries: 3
    restart: unless-stopped

  # RabbitMQ (Message Queue)
  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: clinic-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped

  # Spring Boot Backend
  backend:
    build:
      context: ./clinic-api
      dockerfile: Dockerfile
    container_name: clinic-backend
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/clinic
      SPRING_DATASOURCE_USERNAME: clinic_user
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PASSWORD: ${REDIS_PASSWORD}
      MINIO_ENDPOINT: http://minio:9000
      MINIO_ACCESS_KEY: ${MINIO_ROOT_USER}
      MINIO_SECRET_KEY: ${MINIO_ROOT_PASSWORD}
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_USERNAME: ${RABBITMQ_USER}
      RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      minio:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    restart: unless-stopped

  # React Frontend
  frontend:
    build:
      context: ./clinic-web
      dockerfile: Dockerfile
    container_name: clinic-frontend
    ports:
      - "3000:80"
    depends_on:
      - backend
    restart: unless-stopped

  # Nginx Reverse Proxy
  nginx:
    image: nginx:alpine
    container_name: clinic-nginx
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
      - ./nginx/html:/usr/share/nginx/html:ro
    ports:
      - "80:80"
      - "443:443"
    depends_on:
      - backend
      - frontend
    restart: unless-stopped

  # Prometheus (Metrics)
  prometheus:
    image: prom/prometheus:latest
    container_name: clinic-prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=30d'
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus_data:/prometheus
    ports:
      - "9090:9090"
    restart: unless-stopped

  # Grafana (Visualization)
  grafana:
    image: grafana/grafana:latest
    container_name: clinic-grafana
    environment:
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD}
      GF_INSTALL_PLUGINS: grafana-piechart-panel
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning:ro
    ports:
      - "3001:3000"
    depends_on:
      - prometheus
    restart: unless-stopped

  # Elasticsearch (Logging)
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: clinic-elasticsearch
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
      - xpack.security.enabled=false
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    ports:
      - "9200:9200"
    restart: unless-stopped

  # Logstash
  logstash:
    image: docker.elastic.co/logstash/logstash:8.11.0
    container_name: clinic-logstash
    volumes:
      - ./logstash/pipeline:/usr/share/logstash/pipeline:ro
    ports:
      - "5000:5000"
      - "9600:9600"
    depends_on:
      - elasticsearch
    restart: unless-stopped

  # Kibana
  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    container_name: clinic-kibana
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
    ports:
      - "5601:5601"
    depends_on:
      - elasticsearch
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:
  minio_data:
  rabbitmq_data:
  prometheus_data:
  grafana_data:
  elasticsearch_data:
```

### Environment Variables (.env)

```bash
# Database
DB_PASSWORD=your_secure_db_password

# Redis
REDIS_PASSWORD=your_secure_redis_password

# MinIO
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=your_secure_minio_password

# RabbitMQ
RABBITMQ_USER=clinic_user
RABBITMQ_PASSWORD=your_secure_rabbitmq_password

# JWT
JWT_SECRET=your_very_long_jwt_secret_key_min_256_bits

# Grafana
GRAFANA_PASSWORD=your_secure_grafana_password
```

### Nginx Configuration

**nginx/nginx.conf:**
```nginx
events {
    worker_connections 1024;
}

http {
    upstream backend {
        server backend:8080;
    }

    upstream frontend {
        server frontend:80;
    }

    # HTTP to HTTPS redirect
    server {
        listen 80;
        server_name clinic.example.com;
        return 301 https://$server_name$request_uri;
    }

    # HTTPS server
    server {
        listen 443 ssl http2;
        server_name clinic.example.com;

        ssl_certificate /etc/nginx/ssl/fullchain.pem;
        ssl_certificate_key /etc/nginx/ssl/privkey.pem;
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers HIGH:!aNULL:!MD5;

        # Security headers
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-XSS-Protection "1; mode=block" always;
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

        # Frontend
        location / {
            proxy_pass http://frontend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Backend API
        location /api/ {
            proxy_pass http://backend/api/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;

            # WebSocket support
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
        }

        # Health check endpoint
        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }
    }
}
```

### Automated Backup Script

**scripts/backup.sh:**
```bash
#!/bin/bash

# Configuration
BACKUP_DIR="/backups"
RETENTION_DAYS=30
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# PostgreSQL backup
docker exec clinic-postgres pg_dump -U clinic_user clinic | gzip > \
  ${BACKUP_DIR}/db_backup_${TIMESTAMP}.sql.gz

# MinIO backup (export bucket)
docker exec clinic-minio mc mirror /data/clinic-documents \
  ${BACKUP_DIR}/minio_backup_${TIMESTAMP}

# Compress MinIO backup
tar -czf ${BACKUP_DIR}/minio_backup_${TIMESTAMP}.tar.gz \
  ${BACKUP_DIR}/minio_backup_${TIMESTAMP}
rm -rf ${BACKUP_DIR}/minio_backup_${TIMESTAMP}

# Remove old backups
find ${BACKUP_DIR} -name "*.gz" -mtime +${RETENTION_DAYS} -delete

echo "Backup completed: ${TIMESTAMP}"
```

**Cron job (add to crontab):**
```bash
# Daily backup at 2 AM
0 2 * * * /opt/clinic/scripts/backup.sh >> /var/log/clinic-backup.log 2>&1
```

### SSL Certificate Setup (Let's Encrypt)

```bash
# Install Certbot
sudo apt update
sudo apt install certbot

# Obtain certificate
sudo certbot certonly --standalone -d clinic.example.com \
  --email admin@example.com --agree-tos --non-interactive

# Copy certificates to nginx directory
sudo cp /etc/letsencrypt/live/clinic.example.com/fullchain.pem ./nginx/ssl/
sudo cp /etc/letsencrypt/live/clinic.example.com/privkey.pem ./nginx/ssl/

# Set up auto-renewal (cron)
0 0 * * * certbot renew --quiet --deploy-hook "docker restart clinic-nginx"
```

### VM Requirements & Specifications

#### GCP Configuration Options (asia-south1 Mumbai)

**Option 1: Development/Testing Environment**
- **Machine Type**: e2-standard-4
- **vCPUs**: 4 vCPUs (shared core)
- **Memory**: 16 GB
- **Boot Disk**: 200 GB SSD (pd-ssd)
- **Use Case**: Development, testing, small clinic (1-2 users)
- **Monthly Cost**: ₹8,500 (on-demand) | ₹5,355 (1-year CUD)
- **Limitations**: No ELK stack, reduced Prometheus retention (7 days), single clinic tenant

**Option 2: Production - Recommended** ⭐
- **Machine Type**: n2-standard-8
- **vCPUs**: 8 vCPUs (dedicated)
- **Memory**: 32 GB
- **Boot Disk**: 500 GB SSD (pd-ssd)
- **Backup Disk**: 200 GB pd-standard
- **Use Case**: Production deployment, 3-5 concurrent clinics, 20-30 active users
- **Monthly Cost**: ₹17,650 (on-demand) | ₹11,120 (1-year CUD) | ₹7,943 (3-year CUD)
- **Annual Cost**: ₹2,11,800 (on-demand) | ₹1,33,440 (1-year) | ₹95,316 (3-year)
- **Features**: Full stack with ELK, 30-day log retention, full monitoring, automated backups

**Option 3: Production - Budget**
- **Machine Type**: e2-highmem-4
- **vCPUs**: 4 vCPUs (shared core)
- **Memory**: 32 GB
- **Boot Disk**: 350 GB SSD (pd-balanced)
- **Use Case**: Production deployment, 2-3 concurrent clinics, 15-20 active users
- **Monthly Cost**: ₹13,200 (on-demand) | ₹8,316 (1-year CUD)
- **Features**: Simplified logging, 15-day Prometheus retention, essential monitoring

**Option 4: Production - Optimal**
- **Machine Type**: n2-highmem-8
- **vCPUs**: 8 vCPUs (dedicated)
- **Memory**: 64 GB
- **Boot Disk**: 1 TB SSD (pd-ssd)
- **Backup Disk**: 500 GB pd-standard
- **Use Case**: Large deployment, 10+ concurrent clinics, 50+ active users
- **Monthly Cost**: ₹26,800 (on-demand) | ₹16,884 (1-year CUD) | ₹12,060 (3-year CUD)
- **Features**: Full stack, 90-day log retention, high-availability ready, extensive caching

#### Resource Requirements Analysis

| Component | RAM Usage | CPU Usage | Disk I/O |
|-----------|-----------|-----------|----------|
| PostgreSQL 16 | 2-4 GB | 1-2 cores | High |
| Redis 7 | 512 MB - 1 GB | 0.5 cores | Medium |
| MinIO | 1-2 GB | 0.5-1 cores | High |
| RabbitMQ | 512 MB - 1 GB | 0.5 cores | Medium |
| Spring Boot Backend | 2-4 GB | 2-3 cores | Medium |
| React Frontend (Nginx) | 256 MB | 0.2 cores | Low |
| Prometheus | 1-2 GB | 0.5 cores | Medium |
| Grafana | 512 MB | 0.2 cores | Low |
| Elasticsearch | 2-4 GB | 1-2 cores | High |
| Logstash | 1-2 GB | 0.5-1 cores | Medium |
| Kibana | 512 MB | 0.2 cores | Low |
| Nginx (Reverse Proxy) | 128 MB | 0.1 cores | Low |
| System Overhead | 2 GB | 0.5 cores | - |
| **Total (Minimum)** | **13.5 GB** | **7.7 cores** | - |
| **Total (Comfortable)** | **23 GB** | **12 cores** | - |

#### Storage Requirements

| Component | Storage Requirement |
|-----------|---------------------|
| Operating System (Ubuntu 22.04) | 10 GB |
| Docker Images | 8-10 GB |
| PostgreSQL Data | 20-50 GB (grows over time) |
| MinIO Documents | 50-100 GB (grows over time) |
| Elasticsearch Logs (30 days) | 10-20 GB |
| Prometheus Metrics (30 days) | 5-10 GB |
| Backup Storage | 50-100 GB |
| System Swap | 8 GB |
| Buffer/Free Space (20%) | 40 GB |
| **Total Minimum** | **201 GB** |
| **Recommended** | **350-500 GB** |

#### Cost Summary (GCP asia-south1)

| Configuration | On-Demand Monthly | 1-Year CUD Monthly | Annual Savings (CUD) |
|---------------|-------------------|-------------------|---------------------|
| Dev/Test (e2-standard-4) | ₹8,500 | ₹5,355 | ₹37,740/year |
| Budget Production (e2-highmem-4) | ₹13,200 | ₹8,316 | ₹58,608/year |
| **Recommended (n2-standard-8)** | **₹17,650** | **₹11,120** | **₹78,360/year** |
| Optimal (n2-highmem-8) | ₹26,800 | ₹16,884 | ₹1,18,992/year |

**Note**: Costs include VM instance, storage (SSD + backup), network (static IP + 100GB egress), and basic monitoring. CUD = Committed Use Discount.

#### Cost Optimization Strategies

1. **Use Committed Use Discounts (CUD)**: Save 37% (1-year) or 55% (3-year)
2. **Right-size resources**: Start with e2-highmem-4, monitor for 1 month, scale if needed
3. **Simplify monitoring for small deployments**: Use Cloud Logging instead of ELK → Save ₹3,000-4,000/month
4. **Use pd-balanced instead of pd-ssd for non-critical data**: Save 40% on storage costs
5. **Schedule auto-shutdown for dev/test environments**: Save 73% on dev costs
6. **Enable compression (gzip/brotli)**: Reduce bandwidth by 40-50%

**Potential Total Savings**: Up to ₹1,99,620/year (45% reduction)

#### Generic VM Specifications (For Non-GCP Deployments)

**Minimum Specifications:**
- **CPU**: 4 cores
- **RAM**: 16 GB
- **Storage**: 100 GB SSD
- **OS**: Ubuntu 22.04 LTS or RHEL 9
- **Network**: Static IP with open ports 80, 443

**Recommended Specifications:**
- **CPU**: 8 cores (dedicated)
- **RAM**: 32 GB
- **Storage**: 500 GB SSD (with RAID 1 for data redundancy)
- **OS**: Ubuntu 22.04 LTS
- **Network**: Static IP with firewall configured

### Deployment Steps

```bash
# 1. Install Docker and Docker Compose
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo apt install docker-compose-plugin

# 2. Clone repository
git clone <repository-url> /opt/clinic
cd /opt/clinic

# 3. Configure environment
cp .env.example .env
nano .env  # Edit with secure passwords

# 4. Create required directories
mkdir -p nginx/ssl nginx/html backups prometheus grafana logstash/pipeline

# 5. Initialize MinIO buckets
docker-compose up -d minio
# Access MinIO console at http://<vm-ip>:9001
# Create bucket: clinic-documents

# 6. Start all services
docker-compose up -d

# 7. Run database migrations
docker exec clinic-backend ./gradlew flywayMigrate

# 8. Verify health
curl http://localhost:8080/actuator/health
curl http://localhost:3000

# 9. Set up SSL and restart nginx
# Follow SSL certificate setup above
docker-compose restart nginx
```

---

## Development Roadmap

### Phase 1: Foundation & Setup
1.  Database schema design
2.  Project specification documentation
3. 🔲 Initialize Gradle project with Spring Boot
4. 🔲 Create Flyway migration scripts
5. 🔲 Set up local development environment (Docker Compose)
6. 🔲 Configure Spring Security basics

### Phase 2: Backend Core
1. 🔲 Create JPA entities (all 23 tables)
2. 🔲 Implement repository layer
3. 🔲 Build service layer with business logic
4. 🔲 Implement state machines
5. 🔲 Set up multi-tenancy (RLS integration)
6. 🔲 Implement audit logging with AOP
7. 🔲 Create REST API controllers
8. 🔲 Set up MapStruct DTOs

### Phase 3: Authentication & Authorization
1. 🔲 JWT token generation & validation
2. 🔲 Login/logout endpoints
3. 🔲 Role-based access control (RBAC)
4. 🔲 Permission checking
5. 🔲 User session management

### Phase 4: Frontend Foundation
1. 🔲 Initialize Vite + React + TypeScript
2. 🔲 Set up Tailwind CSS + shadcn/ui
3. 🔲 Create routing structure
4. 🔲 Implement authentication flow
5. 🔲 Build layout components
6. 🔲 Set up TanStack Query

### Phase 5: Core Features - Patient Management
1. 🔲 Patient registration form
2. 🔲 Patient list with search/filter
3. 🔲 Patient detail view
4. 🔲 Patient edit functionality
5. 🔲 Consent management
6. 🔲 Document upload

### Phase 6: Core Features - Scheduling
1. 🔲 Appointment creation form
2. 🔲 Appointment calendar view
3. 🔲 Appointment list view
4. 🔲 Appointment status updates
5. 🔲 Conflict detection
6. 🔲 Reminder system integration

### Phase 7: Core Features - Clinical
1. 🔲 Encounter creation
2. 🔲 Vital signs recording
3. 🔲 Diagnosis entry
4. 🔲 Prescription writing
5. 🔲 Lab order placement
6. 🔲 Document attachment

### Phase 8: Testing
1. 🔲 Unit tests (backend services)
2. 🔲 Integration tests (repositories)
3. 🔲 API tests (controllers)
4. 🔲 Frontend component tests
5. 🔲 E2E tests

### Phase 9: VM Deployment
1. 🔲 Provision VM (Ubuntu 22.04 LTS)
2. 🔲 Install Docker and Docker Compose
3. 🔲 Configure firewall and security groups
4. 🔲 Set up domain and DNS
5. 🔲 Obtain SSL certificates (Let's Encrypt)
6. 🔲 Configure Docker Compose stack
7. 🔲 Deploy all services (Postgres, Redis, MinIO, RabbitMQ)
8. 🔲 Deploy application containers (backend + frontend)
9. 🔲 Configure Nginx reverse proxy with SSL
10. 🔲 Set up monitoring (Prometheus + Grafana)
11. 🔲 Set up logging (ELK stack)
12. 🔲 Configure automated backups

### Phase 10: Production Readiness
1. 🔲 Performance optimization
2. 🔲 Security hardening
3. 🔲 Compliance verification (DPDP, IT Act)
4. 🔲 User acceptance testing
5. 🔲 Documentation completion
6. 🔲 Training materials

---

## Additional Notes

### Important Constraints

1. **No Patient Portal**: Patients do not have login access. All data entry is done by clinic staff on behalf of patients.

2. **Consent Handling**: Staff record patient consent (verbal or written) using the `patient_consents` table. The system tracks:
   - Consent type (treatment, data processing, communication)
   - Status (pending, given, withdrawn)
   - Timestamps (given_at, withdrawn_at)
   - Staff member who recorded consent

3. **Phone Number Validation**: Removed from database layer (no custom domain). Validation handled at application level for flexibility across different Indian number formats.

4. **License Compliance**: All libraries use permissive open-source licenses (Apache 2.0, MIT, BSD, PostgreSQL license). No GPL dependencies in core application.

5. **Data Localization**: All infrastructure hosted on VM in India. No cross-border data transfer.

6. **Audit Retention**: Audit logs retained for minimum 7 years as per Clinical Establishments Act. Partitioned by month for performance.

### Security Best Practices

1. **Environment Variables**: Never commit secrets to version control
2. **TLS Everywhere**: All communication encrypted (TLS 1.3)
3. **Database Encryption**: Transparent Data Encryption (TDE) enabled
4. **Least Privilege**: IAM roles with minimal necessary permissions
5. **Input Validation**: All user input validated and sanitized
6. **SQL Injection Prevention**: QueryDSL and parameterized queries only
7. **XSS Prevention**: Content Security Policy headers
8. **CSRF Protection**: Token-based CSRF protection

### Performance Considerations

1. **Database Indexes**: Comprehensive indexing strategy (see schema)
2. **Caching**: Redis for session data and frequently accessed reference data
3. **Pagination**: All list endpoints support pagination
4. **Connection Pooling**: HikariCP for optimal connection management
5. **Query Optimization**: N+1 query prevention with JOIN FETCH
6. **CDN**: Static assets served via Cloud CDN

### Monitoring & Observability

1. **Cloud Logging**: Centralized log aggregation
2. **Cloud Monitoring**: Custom dashboards for key metrics
3. **Cloud Trace**: Distributed tracing for performance analysis
4. **Alerting**: Automated alerts for errors, performance degradation
5. **Audit Dashboard**: Real-time audit log visualization

---

## Document Version

**Version**: 1.0
**Last Updated**: 2026-01-15
**Author**: System Architect
**Status**: Final

---

## References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [PostgreSQL 16 Documentation](https://www.postgresql.org/docs/16/)
- [React Documentation](https://react.dev/)
- [Docker Documentation](https://docs.docker.com/)
- [MinIO Documentation](https://min.io/docs/minio/linux/index.html)
- [DPDP Act 2023](https://www.meity.gov.in/writereaddata/files/Digital%20Personal%20Data%20Protection%20Act%202023.pdf)
- [ABDM Guidelines](https://abdm.gov.in/)
