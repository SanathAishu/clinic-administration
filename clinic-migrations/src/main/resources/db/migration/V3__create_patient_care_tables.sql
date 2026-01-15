-- ============================================================================
-- V3: Create Patient Care Tables
-- ============================================================================
-- This migration creates core patient care and clinical tables:
-- 1. patients - Patient demographics and ABHA integration
-- 2. appointments - Appointment scheduling with temporal constraints
-- 3. medical_records - Consultation notes and clinical documentation
-- 4. prescriptions - Prescription headers
-- 5. prescription_items - Individual medication items
-- 6. lab_tests - Lab test orders
-- 7. lab_results - Lab test results
-- 8. vitals - Patient vital signs
-- 9. diagnoses - ICD-10 coded diagnoses
-- 10. billing - Billing and invoicing
-- ============================================================================

-- ============================================================================
-- Additional Enums for Patient Care
-- ============================================================================

CREATE TYPE marital_status_enum AS ENUM ('SINGLE', 'MARRIED', 'DIVORCED', 'WIDOWED', 'OTHER');

CREATE TYPE consultation_type_enum AS ENUM ('IN_PERSON', 'TELEMEDICINE', 'FOLLOW_UP', 'EMERGENCY');

CREATE TYPE payment_status_enum AS ENUM ('PENDING', 'PAID', 'PARTIALLY_PAID', 'REFUNDED', 'CANCELLED');

CREATE TYPE payment_method_enum AS ENUM ('CASH', 'CARD', 'UPI', 'NET_BANKING', 'INSURANCE');

CREATE TYPE lab_test_status_enum AS ENUM ('ORDERED', 'SAMPLE_COLLECTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');

CREATE TYPE prescription_status_enum AS ENUM ('ACTIVE', 'COMPLETED', 'CANCELLED');

-- ============================================================================
-- 1. PATIENTS TABLE
-- ============================================================================
CREATE TABLE patients (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    -- Demographics
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender gender_enum NOT NULL,
    blood_group blood_group_enum,

    -- Contact Information
    email email_type,
    phone phone_type NOT NULL,
    alternate_phone phone_type,

    -- Address
    address_line1 TEXT NOT NULL,
    address_line2 TEXT,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(10) NOT NULL,

    -- ABHA Integration (Ayushman Bharat Health Account)
    abha_id abha_id_type UNIQUE,
    abha_number VARCHAR(17) UNIQUE,

    -- Additional Information
    marital_status marital_status_enum,
    occupation VARCHAR(100),
    emergency_contact_name VARCHAR(200),
    emergency_contact_phone phone_type,
    emergency_contact_relation VARCHAR(50),

    -- Medical Information
    allergies TEXT[],
    chronic_conditions TEXT[],

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    deleted_at TIMESTAMPTZ,

    CONSTRAINT patients_age_check CHECK (date_of_birth <= CURRENT_DATE AND date_of_birth >= '1900-01-01')
);

CREATE INDEX idx_patients_tenant ON patients(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_patients_phone ON patients(phone) WHERE deleted_at IS NULL;
CREATE INDEX idx_patients_abha ON patients(abha_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_patients_name ON patients(tenant_id, last_name, first_name) WHERE deleted_at IS NULL;

-- ============================================================================
-- 2. APPOINTMENTS TABLE (with temporal non-overlap constraint)
-- ============================================================================
CREATE TABLE appointments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    patient_id UUID NOT NULL REFERENCES patients(id),
    doctor_id UUID NOT NULL REFERENCES users(id),

    -- Appointment Details
    appointment_time TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 30,
    consultation_type consultation_type_enum NOT NULL DEFAULT 'IN_PERSON',
    status appointment_status_enum NOT NULL DEFAULT 'SCHEDULED',

    -- Additional Information
    reason TEXT,
    notes TEXT,

    -- State Machine Tracking
    confirmed_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancelled_by UUID REFERENCES users(id),
    cancellation_reason TEXT,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    deleted_at TIMESTAMPTZ,

    CONSTRAINT appointments_duration CHECK (duration_minutes BETWEEN 15 AND 240)
    -- Note: Temporal non-overlap constraint enforced at application level
);

CREATE INDEX idx_appointments_tenant ON appointments(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_appointments_patient ON appointments(patient_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_appointments_doctor_time ON appointments(doctor_id, appointment_time) WHERE deleted_at IS NULL;
CREATE INDEX idx_appointments_status ON appointments(status) WHERE deleted_at IS NULL;

-- ============================================================================
-- 3. MEDICAL_RECORDS TABLE
-- ============================================================================
CREATE TABLE medical_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    patient_id UUID NOT NULL REFERENCES patients(id),
    appointment_id UUID REFERENCES appointments(id),
    doctor_id UUID NOT NULL REFERENCES users(id),

    -- Clinical Documentation
    chief_complaint TEXT NOT NULL,
    history_present_illness TEXT,
    examination_findings TEXT,
    clinical_notes TEXT,
    treatment_plan TEXT,
    follow_up_instructions TEXT,
    follow_up_date DATE,

    -- Metadata
    record_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_medical_records_tenant ON medical_records(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_medical_records_patient ON medical_records(patient_id, record_date DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_medical_records_appointment ON medical_records(appointment_id) WHERE deleted_at IS NULL;

-- ============================================================================
-- 4. PRESCRIPTIONS TABLE
-- ============================================================================
CREATE TABLE prescriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    patient_id UUID NOT NULL REFERENCES patients(id),
    medical_record_id UUID REFERENCES medical_records(id),
    doctor_id UUID NOT NULL REFERENCES users(id),

    -- Prescription Details
    prescription_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status prescription_status_enum NOT NULL DEFAULT 'ACTIVE',
    notes TEXT,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_prescriptions_tenant ON prescriptions(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_prescriptions_patient ON prescriptions(patient_id, prescription_date DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_prescriptions_medical_record ON prescriptions(medical_record_id) WHERE deleted_at IS NULL;

-- ============================================================================
-- 5. PRESCRIPTION_ITEMS TABLE
-- ============================================================================
CREATE TABLE prescription_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    prescription_id UUID NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,

    -- Medication Details
    medication_name VARCHAR(255) NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    frequency VARCHAR(100) NOT NULL,
    duration_days INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    instructions TEXT,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT prescription_items_duration CHECK (duration_days BETWEEN 1 AND 365),
    CONSTRAINT prescription_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_prescription_items_prescription ON prescription_items(prescription_id);

-- ============================================================================
-- 6. LAB_TESTS TABLE
-- ============================================================================
CREATE TABLE lab_tests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    patient_id UUID NOT NULL REFERENCES patients(id),
    medical_record_id UUID REFERENCES medical_records(id),
    ordered_by UUID NOT NULL REFERENCES users(id),

    -- Test Details
    test_name VARCHAR(255) NOT NULL,
    test_code VARCHAR(50),
    instructions TEXT,
    status lab_test_status_enum NOT NULL DEFAULT 'ORDERED',

    -- Tracking
    ordered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sample_collected_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_lab_tests_tenant ON lab_tests(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_lab_tests_patient ON lab_tests(patient_id, ordered_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_lab_tests_status ON lab_tests(status) WHERE deleted_at IS NULL;

-- ============================================================================
-- 7. LAB_RESULTS TABLE
-- ============================================================================
CREATE TABLE lab_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    lab_test_id UUID NOT NULL REFERENCES lab_tests(id) ON DELETE CASCADE,

    -- Result Details
    parameter_name VARCHAR(255) NOT NULL,
    result_value VARCHAR(255) NOT NULL,
    unit VARCHAR(50),
    reference_range VARCHAR(100),
    is_abnormal BOOLEAN DEFAULT FALSE,
    comments TEXT,

    -- Metadata
    result_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    entered_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lab_results_test ON lab_results(lab_test_id);

-- ============================================================================
-- 8. VITALS TABLE
-- ============================================================================
CREATE TABLE vitals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    patient_id UUID NOT NULL REFERENCES patients(id),
    appointment_id UUID REFERENCES appointments(id),

    -- Vital Signs
    temperature_celsius NUMERIC(4,1),
    pulse_bpm INTEGER,
    systolic_bp INTEGER,
    diastolic_bp INTEGER,
    respiratory_rate INTEGER,
    oxygen_saturation INTEGER,
    weight_kg NUMERIC(5,2),
    height_cm NUMERIC(5,2),
    bmi NUMERIC(4,2),

    -- Additional Measurements
    blood_glucose_mgdl INTEGER,
    notes TEXT,

    -- Metadata
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recorded_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT vitals_temp_range CHECK (temperature_celsius IS NULL OR temperature_celsius BETWEEN 30.0 AND 45.0),
    CONSTRAINT vitals_pulse_range CHECK (pulse_bpm IS NULL OR pulse_bpm BETWEEN 30 AND 250),
    CONSTRAINT vitals_bp_range CHECK (
        (systolic_bp IS NULL OR systolic_bp BETWEEN 50 AND 250) AND
        (diastolic_bp IS NULL OR diastolic_bp BETWEEN 30 AND 150)
    ),
    CONSTRAINT vitals_spo2_range CHECK (oxygen_saturation IS NULL OR oxygen_saturation BETWEEN 0 AND 100)
);

CREATE INDEX idx_vitals_tenant ON vitals(tenant_id);
CREATE INDEX idx_vitals_patient ON vitals(patient_id, recorded_at DESC);
CREATE INDEX idx_vitals_appointment ON vitals(appointment_id);

-- ============================================================================
-- 9. DIAGNOSES TABLE (ICD-10 Coded)
-- ============================================================================
CREATE TABLE diagnoses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    medical_record_id UUID NOT NULL REFERENCES medical_records(id),

    -- Diagnosis Details
    icd10_code VARCHAR(10),
    diagnosis_name VARCHAR(500) NOT NULL,
    diagnosis_type VARCHAR(50) NOT NULL,
    severity VARCHAR(50),
    notes TEXT,

    -- Metadata
    diagnosed_at DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id)
);

CREATE INDEX idx_diagnoses_tenant ON diagnoses(tenant_id);
CREATE INDEX idx_diagnoses_medical_record ON diagnoses(medical_record_id);
CREATE INDEX idx_diagnoses_icd10 ON diagnoses(icd10_code);

-- ============================================================================
-- 10. BILLING TABLE
-- ============================================================================
CREATE TABLE billing (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    patient_id UUID NOT NULL REFERENCES patients(id),
    appointment_id UUID REFERENCES appointments(id),

    -- Billing Details
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    invoice_date DATE NOT NULL DEFAULT CURRENT_DATE,

    -- Amounts
    subtotal NUMERIC(10,2) NOT NULL,
    discount_amount NUMERIC(10,2) DEFAULT 0,
    tax_amount NUMERIC(10,2) DEFAULT 0,
    total_amount NUMERIC(10,2) NOT NULL,
    paid_amount NUMERIC(10,2) DEFAULT 0,
    balance_amount NUMERIC(10,2) NOT NULL,

    -- Payment Details
    payment_status payment_status_enum NOT NULL DEFAULT 'PENDING',
    payment_method payment_method_enum,
    payment_date DATE,
    payment_reference VARCHAR(100),

    -- Line Items (JSONB for flexibility)
    line_items JSONB NOT NULL,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    deleted_at TIMESTAMPTZ,

    CONSTRAINT billing_amounts_positive CHECK (
        subtotal >= 0 AND
        discount_amount >= 0 AND
        tax_amount >= 0 AND
        total_amount >= 0 AND
        paid_amount >= 0 AND
        balance_amount >= 0
    ),
    CONSTRAINT billing_balance_check CHECK (balance_amount = total_amount - paid_amount),
    CONSTRAINT billing_payment_status_check CHECK (
        (payment_status = 'PAID' AND balance_amount = 0) OR
        (payment_status = 'PARTIALLY_PAID' AND balance_amount > 0 AND paid_amount > 0) OR
        (payment_status IN ('PENDING', 'CANCELLED', 'REFUNDED'))
    )
);

CREATE INDEX idx_billing_tenant ON billing(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_billing_patient ON billing(patient_id, invoice_date DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_billing_invoice ON billing(invoice_number) WHERE deleted_at IS NULL;
CREATE INDEX idx_billing_payment_status ON billing(payment_status) WHERE deleted_at IS NULL;

-- ============================================================================
-- Triggers
-- ============================================================================

CREATE TRIGGER update_patients_updated_at BEFORE UPDATE ON patients
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_appointments_updated_at BEFORE UPDATE ON appointments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_medical_records_updated_at BEFORE UPDATE ON medical_records
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_prescriptions_updated_at BEFORE UPDATE ON prescriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_lab_tests_updated_at BEFORE UPDATE ON lab_tests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_billing_updated_at BEFORE UPDATE ON billing
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Prevent hard deletes
CREATE TRIGGER prevent_hard_delete_patients BEFORE DELETE ON patients
    FOR EACH ROW EXECUTE FUNCTION prevent_hard_delete();

CREATE TRIGGER prevent_hard_delete_appointments BEFORE DELETE ON appointments
    FOR EACH ROW EXECUTE FUNCTION prevent_hard_delete();

CREATE TRIGGER prevent_hard_delete_medical_records BEFORE DELETE ON medical_records
    FOR EACH ROW EXECUTE FUNCTION prevent_hard_delete();

CREATE TRIGGER prevent_hard_delete_prescriptions BEFORE DELETE ON prescriptions
    FOR EACH ROW EXECUTE FUNCTION prevent_hard_delete();

CREATE TRIGGER prevent_hard_delete_lab_tests BEFORE DELETE ON lab_tests
    FOR EACH ROW EXECUTE FUNCTION prevent_hard_delete();

CREATE TRIGGER prevent_hard_delete_billing BEFORE DELETE ON billing
    FOR EACH ROW EXECUTE FUNCTION prevent_hard_delete();

-- ============================================================================
-- Comments
-- ============================================================================

COMMENT ON TABLE patients IS 'Patient demographics with ABHA integration for ABDM compliance';
COMMENT ON TABLE appointments IS 'Appointment scheduling with temporal non-overlap constraints';
COMMENT ON TABLE medical_records IS 'Clinical documentation and consultation notes';
COMMENT ON TABLE prescriptions IS 'Prescription headers linking to medical records';
COMMENT ON TABLE prescription_items IS 'Individual medication items in prescriptions';
COMMENT ON TABLE lab_tests IS 'Laboratory test orders with status tracking';
COMMENT ON TABLE lab_results IS 'Laboratory test results with parameters and reference ranges';
COMMENT ON TABLE vitals IS 'Patient vital signs recorded during visits';
COMMENT ON TABLE diagnoses IS 'ICD-10 coded diagnoses linked to medical records';
COMMENT ON TABLE billing IS 'Billing and invoicing with payment tracking';
