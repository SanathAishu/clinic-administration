-- =====================================================================================
-- Migration: V6__create_read_views.sql
-- Description: Create database views for optimized READ operations (CQRS pattern)
-- Author: System
-- Date: 2026-01-15
--
-- Architecture: CQRS Pattern
-- - Commands (CREATE, UPDATE, DELETE): Handled by JPA entities with relationships
-- - Queries (READ): Handled by database views leveraging DB engine optimization
--
-- Benefits:
-- - Pre-joined data reduces N+1 query problems
-- - Database engine optimizes complex JOINs
-- - Simpler application code (no complex JPQL/HQL)
-- - Better separation of concerns
-- =====================================================================================

-- =====================================================================================
-- 1. PATIENT DOMAIN VIEWS
-- =====================================================================================

-- v_patient_list: Patient listing with computed fields
CREATE OR REPLACE VIEW v_patient_list AS
SELECT
    p.id,
    p.tenant_id,
    p.first_name,
    p.middle_name,
    p.last_name,
    p.first_name || COALESCE(' ' || p.middle_name, '') || ' ' || p.last_name AS full_name,
    p.date_of_birth,
    EXTRACT(YEAR FROM AGE(p.date_of_birth))::INTEGER AS age,
    p.gender,
    p.phone,
    p.email,
    p.blood_group,
    p.abha_id,
    p.created_at,
    p.updated_at,
    CASE WHEN p.deleted_at IS NOT NULL THEN 'INACTIVE' ELSE 'ACTIVE' END AS status,
    p.deleted_at IS NULL AS is_active
FROM patients p;

COMMENT ON VIEW v_patient_list IS 'Patient listing with computed full name, age, and status flags';

-- v_patient_detail: Full patient details with latest vitals
CREATE OR REPLACE VIEW v_patient_detail AS
SELECT
    p.id,
    p.tenant_id,
    p.first_name,
    p.middle_name,
    p.last_name,
    p.first_name || COALESCE(' ' || p.middle_name, '') || ' ' || p.last_name AS full_name,
    p.date_of_birth,
    EXTRACT(YEAR FROM AGE(p.date_of_birth))::INTEGER AS age,
    p.gender,
    p.phone,
    p.email,
    p.address_line1,
    p.address_line2,
    p.city,
    p.state,
    p.pincode,
    p.blood_group,
    p.abha_id,
    p.abha_number,
    p.marital_status,
    p.occupation,
    p.emergency_contact_name,
    p.emergency_contact_phone,
    p.emergency_contact_relation,
    p.allergies,
    p.chronic_conditions,
    p.created_at,
    p.updated_at,
    p.deleted_at,
    -- Latest vital signs
    lv.id AS latest_vital_id,
    lv.recorded_at AS latest_vital_recorded_at,
    lv.temperature_celsius,
    lv.pulse_bpm,
    lv.systolic_bp,
    lv.diastolic_bp,
    lv.respiratory_rate,
    lv.oxygen_saturation,
    lv.weight_kg,
    lv.height_cm,
    lv.bmi,
    CASE WHEN lv.systolic_bp IS NOT NULL AND lv.diastolic_bp IS NOT NULL
         THEN lv.systolic_bp || '/' || lv.diastolic_bp END AS blood_pressure,
    -- Counts
    (SELECT COUNT(*) FROM appointments a WHERE a.patient_id = p.id AND a.tenant_id = p.tenant_id AND a.deleted_at IS NULL) AS total_appointments,
    (SELECT COUNT(*) FROM medical_records mr WHERE mr.patient_id = p.id AND mr.tenant_id = p.tenant_id AND mr.deleted_at IS NULL) AS total_medical_records,
    (SELECT COUNT(*) FROM prescriptions pr WHERE pr.patient_id = p.id AND pr.tenant_id = p.tenant_id AND pr.deleted_at IS NULL) AS total_prescriptions,
    (SELECT COUNT(*) FROM lab_tests lt WHERE lt.patient_id = p.id AND lt.tenant_id = p.tenant_id AND lt.deleted_at IS NULL) AS total_lab_tests,
    (SELECT COALESCE(SUM(b.balance_amount), 0) FROM billing b WHERE b.patient_id = p.id AND b.tenant_id = p.tenant_id AND b.deleted_at IS NULL AND b.balance_amount > 0) AS outstanding_balance
FROM patients p
LEFT JOIN LATERAL (
    SELECT * FROM vitals v
    WHERE v.patient_id = p.id AND v.tenant_id = p.tenant_id
    ORDER BY v.recorded_at DESC LIMIT 1
) lv ON true;

COMMENT ON VIEW v_patient_detail IS 'Comprehensive patient profile with latest vitals and summary counts';

-- v_patient_appointments: Patient appointment history
CREATE OR REPLACE VIEW v_patient_appointments AS
SELECT
    a.id,
    a.tenant_id,
    a.patient_id,
    p.first_name || ' ' || p.last_name AS patient_name,
    p.phone AS patient_phone,
    a.doctor_id,
    d.first_name || ' ' || d.last_name AS doctor_name,
    d.email AS doctor_email,
    a.appointment_time,
    a.appointment_time::DATE AS appointment_date,
    TO_CHAR(a.appointment_time, 'HH24:MI') AS appointment_time_only,
    a.duration_minutes,
    a.appointment_time + (a.duration_minutes || ' minutes')::INTERVAL AS end_time,
    a.consultation_type,
    a.status,
    a.reason,
    a.notes,
    a.confirmed_at,
    a.started_at,
    a.completed_at,
    a.cancelled_at,
    a.cancellation_reason,
    a.created_at,
    a.updated_at,
    mr.id AS medical_record_id,
    mr.chief_complaint,
    CASE WHEN a.status = 'SCHEDULED' AND a.appointment_time < NOW() THEN true ELSE false END AS is_overdue,
    CASE WHEN a.status IN ('SCHEDULED', 'CONFIRMED') AND a.appointment_time::DATE = CURRENT_DATE THEN true ELSE false END AS is_today
FROM appointments a
JOIN patients p ON a.patient_id = p.id AND p.deleted_at IS NULL
JOIN users d ON a.doctor_id = d.id AND d.deleted_at IS NULL
LEFT JOIN medical_records mr ON mr.appointment_id = a.id AND mr.deleted_at IS NULL
WHERE a.deleted_at IS NULL;

COMMENT ON VIEW v_patient_appointments IS 'Patient appointments with doctor details and related medical record';

-- v_patient_medical_history: Complete medical history
CREATE OR REPLACE VIEW v_patient_medical_history AS
SELECT
    mr.id,
    mr.tenant_id,
    mr.patient_id,
    p.first_name || ' ' || p.last_name AS patient_name,
    mr.doctor_id,
    d.first_name || ' ' || d.last_name AS doctor_name,
    mr.record_date,
    mr.chief_complaint,
    mr.history_present_illness,
    mr.examination_findings,
    mr.clinical_notes,
    mr.treatment_plan,
    mr.follow_up_instructions,
    mr.follow_up_date,
    mr.appointment_id,
    a.appointment_time,
    a.consultation_type,
    (SELECT COUNT(*) FROM diagnoses dg WHERE dg.medical_record_id = mr.id AND dg.tenant_id = mr.tenant_id) AS diagnosis_count,
    (SELECT COUNT(*) FROM prescriptions pr WHERE pr.medical_record_id = mr.id AND pr.deleted_at IS NULL) AS prescription_count,
    (SELECT COUNT(*) FROM lab_tests lt WHERE lt.medical_record_id = mr.id AND lt.deleted_at IS NULL) AS lab_test_count,
    mr.created_at,
    mr.updated_at
FROM medical_records mr
JOIN patients p ON mr.patient_id = p.id AND p.deleted_at IS NULL
JOIN users d ON mr.doctor_id = d.id AND d.deleted_at IS NULL
LEFT JOIN appointments a ON mr.appointment_id = a.id
WHERE mr.deleted_at IS NULL;

COMMENT ON VIEW v_patient_medical_history IS 'Patient medical records with doctor info and related item counts';

-- v_patient_billing_history: Patient billing and payment history
CREATE OR REPLACE VIEW v_patient_billing_history AS
SELECT
    b.id,
    b.tenant_id,
    b.patient_id,
    p.first_name || ' ' || p.last_name AS patient_name,
    p.phone AS patient_phone,
    b.invoice_number,
    b.invoice_date,
    b.subtotal,
    b.discount_amount,
    b.tax_amount,
    b.total_amount,
    b.paid_amount,
    b.balance_amount,
    b.payment_status,
    b.payment_method,
    b.payment_date,
    b.payment_reference,
    b.line_items,
    b.appointment_id,
    a.appointment_time,
    a.consultation_type,
    d.id AS doctor_id,
    d.first_name || ' ' || d.last_name AS doctor_name,
    CASE WHEN b.payment_status = 'PENDING' AND b.invoice_date < CURRENT_DATE - INTERVAL '30 days' THEN true ELSE false END AS is_overdue,
    b.balance_amount > 0 AS has_balance,
    b.created_at,
    b.updated_at
FROM billing b
JOIN patients p ON b.patient_id = p.id AND p.deleted_at IS NULL
LEFT JOIN appointments a ON b.appointment_id = a.id
LEFT JOIN users d ON a.doctor_id = d.id
WHERE b.deleted_at IS NULL;

COMMENT ON VIEW v_patient_billing_history IS 'Patient billing records with payment status and appointment details';

-- =====================================================================================
-- 2. CLINICAL DOMAIN VIEWS
-- =====================================================================================

-- v_appointment_list: Appointments with patient and doctor info
CREATE OR REPLACE VIEW v_appointment_list AS
SELECT
    a.id,
    a.tenant_id,
    a.patient_id,
    p.first_name || ' ' || p.last_name AS patient_name,
    p.phone AS patient_phone,
    p.email AS patient_email,
    EXTRACT(YEAR FROM AGE(p.date_of_birth))::INTEGER AS patient_age,
    p.gender AS patient_gender,
    a.doctor_id,
    d.first_name || ' ' || d.last_name AS doctor_name,
    d.email AS doctor_email,
    a.appointment_time,
    a.appointment_time::DATE AS appointment_date,
    TO_CHAR(a.appointment_time, 'HH24:MI') AS start_time,
    TO_CHAR(a.appointment_time + (a.duration_minutes || ' minutes')::INTERVAL, 'HH24:MI') AS end_time,
    a.duration_minutes,
    a.consultation_type,
    a.status,
    a.reason,
    CASE
        WHEN a.appointment_time::DATE = CURRENT_DATE THEN 'TODAY'
        WHEN a.appointment_time::DATE = CURRENT_DATE + 1 THEN 'TOMORROW'
        WHEN a.appointment_time::DATE < CURRENT_DATE THEN 'PAST'
        ELSE 'UPCOMING'
    END AS time_category,
    CASE WHEN a.status = 'SCHEDULED' AND a.appointment_time < NOW() THEN true ELSE false END AS is_overdue,
    a.created_at,
    a.updated_at
FROM appointments a
JOIN patients p ON a.patient_id = p.id AND p.deleted_at IS NULL
JOIN users d ON a.doctor_id = d.id AND d.deleted_at IS NULL
WHERE a.deleted_at IS NULL;

COMMENT ON VIEW v_appointment_list IS 'Appointment listing with patient and doctor details for scheduling screens';

-- v_appointment_detail: Full appointment details
CREATE OR REPLACE VIEW v_appointment_detail AS
SELECT
    a.id,
    a.tenant_id,
    a.patient_id,
    p.first_name AS patient_first_name,
    p.last_name AS patient_last_name,
    p.first_name || ' ' || p.last_name AS patient_name,
    p.phone AS patient_phone,
    p.email AS patient_email,
    p.date_of_birth AS patient_dob,
    EXTRACT(YEAR FROM AGE(p.date_of_birth))::INTEGER AS patient_age,
    p.gender AS patient_gender,
    p.blood_group AS patient_blood_group,
    p.abha_id AS patient_abha_id,
    a.doctor_id,
    d.first_name AS doctor_first_name,
    d.last_name AS doctor_last_name,
    d.first_name || ' ' || d.last_name AS doctor_name,
    d.email AS doctor_email,
    a.appointment_time,
    a.appointment_time::DATE AS appointment_date,
    TO_CHAR(a.appointment_time, 'HH24:MI') AS appointment_time_only,
    a.duration_minutes,
    a.appointment_time + (a.duration_minutes || ' minutes')::INTERVAL AS end_time,
    a.consultation_type,
    a.status,
    a.reason,
    a.notes,
    a.confirmed_at,
    a.started_at,
    a.completed_at,
    a.cancelled_at,
    a.cancelled_by,
    cb.first_name || ' ' || cb.last_name AS cancelled_by_name,
    a.cancellation_reason,
    mr.id AS medical_record_id,
    mr.chief_complaint,
    mr.clinical_notes,
    mr.treatment_plan,
    bil.id AS billing_id,
    bil.invoice_number,
    bil.total_amount,
    bil.balance_amount,
    bil.payment_status,
    a.created_at,
    a.updated_at,
    a.created_by,
    c.first_name || ' ' || c.last_name AS created_by_name
FROM appointments a
JOIN patients p ON a.patient_id = p.id AND p.deleted_at IS NULL
JOIN users d ON a.doctor_id = d.id AND d.deleted_at IS NULL
LEFT JOIN users cb ON a.cancelled_by = cb.id
LEFT JOIN users c ON a.created_by = c.id
LEFT JOIN medical_records mr ON mr.appointment_id = a.id AND mr.deleted_at IS NULL
LEFT JOIN billing bil ON bil.appointment_id = a.id AND bil.deleted_at IS NULL
WHERE a.deleted_at IS NULL;

COMMENT ON VIEW v_appointment_detail IS 'Complete appointment details with patient, doctor, medical record, and billing';

-- v_medical_record_detail: Medical record with diagnoses and prescriptions as JSON
CREATE OR REPLACE VIEW v_medical_record_detail AS
SELECT
    mr.id,
    mr.tenant_id,
    mr.patient_id,
    p.first_name || ' ' || p.last_name AS patient_name,
    EXTRACT(YEAR FROM AGE(p.date_of_birth))::INTEGER AS patient_age,
    p.gender AS patient_gender,
    mr.doctor_id,
    d.first_name || ' ' || d.last_name AS doctor_name,
    mr.record_date,
    mr.chief_complaint,
    mr.history_present_illness,
    mr.examination_findings,
    mr.clinical_notes,
    mr.treatment_plan,
    mr.follow_up_instructions,
    mr.follow_up_date,
    mr.appointment_id,
    a.appointment_time,
    a.consultation_type,
    -- Aggregated diagnoses
    (
        SELECT COALESCE(json_agg(json_build_object(
            'id', dg.id,
            'icd10_code', dg.icd10_code,
            'diagnosis_name', dg.diagnosis_name,
            'diagnosis_type', dg.diagnosis_type,
            'severity', dg.severity,
            'notes', dg.notes
        )), '[]'::json)
        FROM diagnoses dg WHERE dg.medical_record_id = mr.id AND dg.tenant_id = mr.tenant_id
    ) AS diagnoses,
    -- Aggregated prescriptions
    (
        SELECT COALESCE(json_agg(json_build_object(
            'id', pr.id,
            'status', pr.status,
            'prescription_date', pr.prescription_date,
            'notes', pr.notes
        )), '[]'::json)
        FROM prescriptions pr WHERE pr.medical_record_id = mr.id AND pr.deleted_at IS NULL
    ) AS prescriptions,
    (SELECT COUNT(*) FROM diagnoses dg WHERE dg.medical_record_id = mr.id) AS diagnosis_count,
    (SELECT COUNT(*) FROM prescriptions pr WHERE pr.medical_record_id = mr.id AND pr.deleted_at IS NULL) AS prescription_count,
    (SELECT COUNT(*) FROM lab_tests lt WHERE lt.medical_record_id = mr.id AND lt.deleted_at IS NULL) AS lab_test_count,
    mr.created_at,
    mr.updated_at,
    mr.created_by,
    c.first_name || ' ' || c.last_name AS created_by_name
FROM medical_records mr
JOIN patients p ON mr.patient_id = p.id AND p.deleted_at IS NULL
JOIN users d ON mr.doctor_id = d.id AND d.deleted_at IS NULL
LEFT JOIN users c ON mr.created_by = c.id
LEFT JOIN appointments a ON mr.appointment_id = a.id
WHERE mr.deleted_at IS NULL;

COMMENT ON VIEW v_medical_record_detail IS 'Medical record with embedded diagnoses and prescriptions as JSON';

-- v_prescription_list: Prescriptions with patient and doctor info
CREATE OR REPLACE VIEW v_prescription_list AS
SELECT
    pr.id,
    pr.tenant_id,
    pr.patient_id,
    p.first_name || ' ' || p.last_name AS patient_name,
    p.phone AS patient_phone,
    EXTRACT(YEAR FROM AGE(p.date_of_birth))::INTEGER AS patient_age,
    pr.doctor_id,
    d.first_name || ' ' || d.last_name AS doctor_name,
    pr.prescription_date,
    pr.status,
    pr.notes,
    pr.medical_record_id,
    mr.chief_complaint,
    (SELECT COUNT(*) FROM prescription_items pi WHERE pi.prescription_id = pr.id) AS item_count,
    (
        SELECT COALESCE(json_agg(json_build_object(
            'id', pi.id,
            'medication_name', pi.medication_name,
            'dosage', pi.dosage,
            'frequency', pi.frequency,
            'duration_days', pi.duration_days,
            'quantity', pi.quantity,
            'instructions', pi.instructions
        ) ORDER BY pi.created_at), '[]'::json)
        FROM prescription_items pi WHERE pi.prescription_id = pr.id
    ) AS items,
    pr.status = 'ACTIVE' AS is_active,
    pr.created_at,
    pr.updated_at
FROM prescriptions pr
JOIN patients p ON pr.patient_id = p.id AND p.deleted_at IS NULL
JOIN users d ON pr.doctor_id = d.id AND d.deleted_at IS NULL
LEFT JOIN medical_records mr ON pr.medical_record_id = mr.id
WHERE pr.deleted_at IS NULL;

COMMENT ON VIEW v_prescription_list IS 'Prescription listing with embedded items as JSON';

-- v_lab_test_list: Lab tests with results summary
CREATE OR REPLACE VIEW v_lab_test_list AS
SELECT
    lt.id,
    lt.tenant_id,
    lt.patient_id,
    p.first_name || ' ' || p.last_name AS patient_name,
    p.phone AS patient_phone,
    EXTRACT(YEAR FROM AGE(p.date_of_birth))::INTEGER AS patient_age,
    p.gender AS patient_gender,
    lt.ordered_by AS doctor_id,
    d.first_name || ' ' || d.last_name AS ordered_by_name,
    lt.test_name,
    lt.test_code,
    lt.instructions,
    lt.status,
    lt.ordered_at,
    lt.sample_collected_at,
    lt.completed_at,
    lt.medical_record_id,
    (SELECT COUNT(*) FROM lab_results lr WHERE lr.lab_test_id = lt.id) AS result_count,
    (SELECT COUNT(*) FROM lab_results lr WHERE lr.lab_test_id = lt.id AND lr.is_abnormal = true) AS abnormal_count,
    lt.status IN ('ORDERED', 'SAMPLE_COLLECTED', 'IN_PROGRESS') AS is_pending,
    lt.status = 'COMPLETED' AND EXISTS(SELECT 1 FROM lab_results lr WHERE lr.lab_test_id = lt.id AND lr.is_abnormal = true) AS has_abnormal_results,
    lt.created_at,
    lt.updated_at
FROM lab_tests lt
JOIN patients p ON lt.patient_id = p.id AND p.deleted_at IS NULL
JOIN users d ON lt.ordered_by = d.id AND d.deleted_at IS NULL
WHERE lt.deleted_at IS NULL;

COMMENT ON VIEW v_lab_test_list IS 'Lab test listing with result summary and abnormal flags';

-- v_lab_test_detail: Lab test with all results
CREATE OR REPLACE VIEW v_lab_test_detail AS
SELECT
    lt.id,
    lt.tenant_id,
    lt.patient_id,
    p.first_name || ' ' || p.last_name AS patient_name,
    p.phone AS patient_phone,
    p.date_of_birth AS patient_dob,
    EXTRACT(YEAR FROM AGE(p.date_of_birth))::INTEGER AS patient_age,
    p.gender AS patient_gender,
    lt.ordered_by AS doctor_id,
    d.first_name || ' ' || d.last_name AS ordered_by_name,
    d.email AS ordered_by_email,
    lt.test_name,
    lt.test_code,
    lt.instructions,
    lt.status,
    lt.ordered_at,
    lt.sample_collected_at,
    lt.completed_at,
    lt.medical_record_id,
    mr.chief_complaint,
    mr.record_date AS medical_record_date,
    (
        SELECT COALESCE(json_agg(json_build_object(
            'id', lr.id,
            'parameter_name', lr.parameter_name,
            'result_value', lr.result_value,
            'unit', lr.unit,
            'reference_range', lr.reference_range,
            'is_abnormal', lr.is_abnormal,
            'comments', lr.comments,
            'result_date', lr.result_date,
            'entered_by', eb.first_name || ' ' || eb.last_name
        ) ORDER BY lr.parameter_name), '[]'::json)
        FROM lab_results lr LEFT JOIN users eb ON lr.entered_by = eb.id WHERE lr.lab_test_id = lt.id
    ) AS results,
    (SELECT COUNT(*) FROM lab_results lr WHERE lr.lab_test_id = lt.id) AS result_count,
    (SELECT COUNT(*) FROM lab_results lr WHERE lr.lab_test_id = lt.id AND lr.is_abnormal = true) AS abnormal_count,
    lt.created_at,
    lt.updated_at
FROM lab_tests lt
JOIN patients p ON lt.patient_id = p.id AND p.deleted_at IS NULL
JOIN users d ON lt.ordered_by = d.id AND d.deleted_at IS NULL
LEFT JOIN medical_records mr ON lt.medical_record_id = mr.id
WHERE lt.deleted_at IS NULL;

COMMENT ON VIEW v_lab_test_detail IS 'Complete lab test details with all results as JSON';

-- v_diagnosis_list: Diagnoses with patient and medical record info
CREATE OR REPLACE VIEW v_diagnosis_list AS
SELECT
    dg.id,
    dg.tenant_id,
    dg.medical_record_id,
    mr.patient_id,
    p.first_name || ' ' || p.last_name AS patient_name,
    EXTRACT(YEAR FROM AGE(p.date_of_birth))::INTEGER AS patient_age,
    p.gender AS patient_gender,
    mr.doctor_id,
    d.first_name || ' ' || d.last_name AS doctor_name,
    dg.icd10_code,
    dg.diagnosis_name,
    dg.diagnosis_type,
    dg.severity,
    dg.notes,
    dg.diagnosed_at,
    mr.record_date,
    mr.chief_complaint,
    dg.created_at
FROM diagnoses dg
JOIN medical_records mr ON dg.medical_record_id = mr.id AND mr.deleted_at IS NULL
JOIN patients p ON mr.patient_id = p.id AND p.deleted_at IS NULL
JOIN users d ON mr.doctor_id = d.id AND d.deleted_at IS NULL;

COMMENT ON VIEW v_diagnosis_list IS 'Diagnosis listing with patient and doctor information';

-- =====================================================================================
-- 3. OPERATIONAL DOMAIN VIEWS
-- =====================================================================================

-- v_billing_list: Billing records with patient and status info
CREATE OR REPLACE VIEW v_billing_list AS
SELECT
    b.id,
    b.tenant_id,
    b.patient_id,
    p.first_name || ' ' || p.last_name AS patient_name,
    p.phone AS patient_phone,
    b.invoice_number,
    b.invoice_date,
    b.subtotal,
    b.discount_amount,
    b.tax_amount,
    b.total_amount,
    b.paid_amount,
    b.balance_amount,
    b.payment_status,
    b.payment_method,
    b.payment_date,
    b.appointment_id,
    a.appointment_time,
    d.first_name || ' ' || d.last_name AS doctor_name,
    CASE
        WHEN b.balance_amount = 0 THEN 'FULLY_PAID'
        WHEN b.paid_amount > 0 AND b.balance_amount > 0 THEN 'PARTIAL'
        WHEN b.invoice_date < CURRENT_DATE - INTERVAL '30 days' AND b.balance_amount > 0 THEN 'OVERDUE'
        ELSE 'PENDING'
    END AS billing_status,
    CASE WHEN b.invoice_date < CURRENT_DATE - INTERVAL '30 days' AND b.balance_amount > 0
         THEN CURRENT_DATE - b.invoice_date ELSE 0 END AS days_overdue,
    b.created_at,
    b.updated_at
FROM billing b
JOIN patients p ON b.patient_id = p.id AND p.deleted_at IS NULL
LEFT JOIN appointments a ON b.appointment_id = a.id
LEFT JOIN users d ON a.doctor_id = d.id
WHERE b.deleted_at IS NULL;

COMMENT ON VIEW v_billing_list IS 'Billing listing with payment status and aging calculations';

-- v_inventory_list: Inventory items with stock status
CREATE OR REPLACE VIEW v_inventory_list AS
SELECT
    i.id,
    i.tenant_id,
    i.item_name,
    i.item_code,
    i.category,
    i.description,
    i.current_stock,
    i.minimum_stock,
    i.unit,
    i.unit_price,
    i.manufacturer,
    i.batch_number,
    i.expiry_date,
    i.location,
    i.current_stock * COALESCE(i.unit_price, 0) AS stock_value,
    CASE
        WHEN i.current_stock <= 0 THEN 'OUT_OF_STOCK'
        WHEN i.current_stock <= i.minimum_stock THEN 'LOW_STOCK'
        WHEN i.expiry_date IS NOT NULL AND i.expiry_date <= CURRENT_DATE + INTERVAL '30 days' THEN 'EXPIRING_SOON'
        WHEN i.expiry_date IS NOT NULL AND i.expiry_date <= CURRENT_DATE THEN 'EXPIRED'
        ELSE 'IN_STOCK'
    END AS stock_status,
    i.current_stock <= i.minimum_stock AS is_low_stock,
    i.current_stock <= 0 AS is_out_of_stock,
    CASE WHEN i.expiry_date IS NOT NULL THEN i.expiry_date - CURRENT_DATE ELSE NULL END AS days_until_expiry,
    (SELECT COUNT(*) FROM inventory_transactions it WHERE it.inventory_id = i.id AND it.created_at >= NOW() - INTERVAL '30 days') AS transactions_last_30_days,
    i.created_at,
    i.updated_at
FROM inventory i
WHERE i.deleted_at IS NULL;

COMMENT ON VIEW v_inventory_list IS 'Inventory listing with stock status and expiry alerts';

-- v_inventory_low_stock: Low stock and expiring items alert view
CREATE OR REPLACE VIEW v_inventory_low_stock AS
SELECT
    i.id,
    i.tenant_id,
    i.item_name,
    i.item_code,
    i.category,
    i.current_stock,
    i.minimum_stock,
    i.unit,
    i.unit_price,
    i.expiry_date,
    i.location,
    CASE
        WHEN i.current_stock <= 0 THEN 'OUT_OF_STOCK'
        WHEN i.current_stock <= i.minimum_stock THEN 'LOW_STOCK'
        WHEN i.expiry_date IS NOT NULL AND i.expiry_date <= CURRENT_DATE THEN 'EXPIRED'
        WHEN i.expiry_date IS NOT NULL AND i.expiry_date <= CURRENT_DATE + INTERVAL '30 days' THEN 'EXPIRING_SOON'
    END AS alert_type,
    CASE
        WHEN i.current_stock <= 0 THEN 1
        WHEN i.expiry_date IS NOT NULL AND i.expiry_date <= CURRENT_DATE THEN 2
        WHEN i.current_stock <= i.minimum_stock THEN 3
        WHEN i.expiry_date IS NOT NULL AND i.expiry_date <= CURRENT_DATE + INTERVAL '30 days' THEN 4
    END AS priority,
    CASE WHEN i.current_stock < i.minimum_stock THEN i.minimum_stock * 2 - i.current_stock ELSE 0 END AS suggested_reorder_quantity,
    i.created_at,
    i.updated_at
FROM inventory i
WHERE i.deleted_at IS NULL
AND (i.current_stock <= i.minimum_stock OR (i.expiry_date IS NOT NULL AND i.expiry_date <= CURRENT_DATE + INTERVAL '30 days'));

COMMENT ON VIEW v_inventory_low_stock IS 'Alert view for low stock and expiring inventory items';

-- v_inventory_transactions: Inventory transactions with item info
CREATE OR REPLACE VIEW v_inventory_transactions AS
SELECT
    it.id,
    it.tenant_id,
    it.inventory_id,
    i.item_name,
    i.item_code,
    i.category,
    i.unit,
    it.transaction_type,
    it.quantity,
    it.stock_before,
    it.stock_after,
    it.reference_type,
    it.reference_id,
    it.notes,
    it.created_by AS performed_by,
    u.first_name || ' ' || u.last_name AS performed_by_name,
    it.stock_after - it.stock_before AS quantity_change,
    CASE WHEN it.transaction_type IN ('PURCHASE', 'RETURN', 'ADJUSTMENT') THEN 'IN' ELSE 'OUT' END AS direction,
    it.created_at
FROM inventory_transactions it
JOIN inventory i ON it.inventory_id = i.id
LEFT JOIN users u ON it.created_by = u.id;

COMMENT ON VIEW v_inventory_transactions IS 'Inventory transaction history with item and user details';

-- v_notification_list: Notifications with user info
CREATE OR REPLACE VIEW v_notification_list AS
SELECT
    n.id,
    n.tenant_id,
    n.user_id,
    u.first_name || ' ' || u.last_name AS user_name,
    u.email AS user_email,
    n.type,
    n.title,
    n.message,
    n.reference_type,
    n.reference_id,
    n.status,
    n.scheduled_at,
    n.sent_at,
    n.read_at,
    n.status != 'READ' AS is_unread,
    n.status = 'PENDING' AS is_pending,
    n.status = 'FAILED' AS is_failed,
    CASE
        WHEN n.scheduled_at > NOW() THEN 'SCHEDULED'
        WHEN n.sent_at IS NOT NULL AND n.read_at IS NULL THEN 'UNREAD'
        WHEN n.read_at IS NOT NULL THEN 'READ'
        ELSE 'PENDING'
    END AS notification_state,
    n.created_at
FROM notifications n
JOIN users u ON n.user_id = u.id AND u.deleted_at IS NULL;

COMMENT ON VIEW v_notification_list IS 'Notification listing with user details and status flags';

-- v_staff_schedule_list: Staff schedules with user info
CREATE OR REPLACE VIEW v_staff_schedule_list AS
SELECT
    ss.id,
    ss.tenant_id,
    ss.user_id,
    u.first_name || ' ' || u.last_name AS staff_name,
    u.email AS staff_email,
    ss.day_of_week,
    CASE ss.day_of_week
        WHEN 0 THEN 'Sunday' WHEN 1 THEN 'Monday' WHEN 2 THEN 'Tuesday'
        WHEN 3 THEN 'Wednesday' WHEN 4 THEN 'Thursday' WHEN 5 THEN 'Friday' WHEN 6 THEN 'Saturday'
    END AS day_name,
    ss.start_time,
    ss.end_time,
    TO_CHAR(ss.start_time, 'HH24:MI') AS start_time_formatted,
    TO_CHAR(ss.end_time, 'HH24:MI') AS end_time_formatted,
    ss.is_available,
    ss.valid_from,
    ss.valid_until,
    ss.break_start_time,
    ss.break_end_time,
    ss.notes,
    EXTRACT(EPOCH FROM (ss.end_time - ss.start_time)) / 3600 AS hours_scheduled,
    CASE
        WHEN ss.valid_until IS NOT NULL AND ss.valid_until < CURRENT_DATE THEN 'EXPIRED'
        WHEN ss.valid_from > CURRENT_DATE THEN 'FUTURE'
        WHEN ss.is_available = false THEN 'UNAVAILABLE'
        ELSE 'ACTIVE'
    END AS schedule_status,
    ss.created_at,
    ss.updated_at
FROM staff_schedules ss
JOIN users u ON ss.user_id = u.id AND u.deleted_at IS NULL
WHERE ss.deleted_at IS NULL;

COMMENT ON VIEW v_staff_schedule_list IS 'Staff schedule listing with day names and availability status';

-- v_consent_records: Consent records with patient info
CREATE OR REPLACE VIEW v_consent_records AS
SELECT
    cr.id,
    cr.tenant_id,
    cr.patient_id,
    p.first_name || ' ' || p.last_name AS patient_name,
    p.phone AS patient_phone,
    p.email AS patient_email,
    cr.consent_type,
    cr.purpose,
    cr.consent_text,
    cr.consent_version,
    cr.status,
    cr.granted_at,
    cr.revoked_at,
    cr.expires_at,
    cr.ip_address,
    cr.user_agent,
    CASE
        WHEN cr.revoked_at IS NOT NULL THEN 'REVOKED'
        WHEN cr.expires_at IS NOT NULL AND cr.expires_at < NOW() THEN 'EXPIRED'
        WHEN cr.status = 'GRANTED' THEN 'ACTIVE'
        ELSE 'PENDING'
    END AS consent_status,
    cr.status = 'GRANTED' AND cr.revoked_at IS NULL AND (cr.expires_at IS NULL OR cr.expires_at > NOW()) AS is_valid,
    cr.created_at,
    cr.updated_at
FROM consent_records cr
JOIN patients p ON cr.patient_id = p.id AND p.deleted_at IS NULL;

COMMENT ON VIEW v_consent_records IS 'Consent records with patient details and validity status';

-- =====================================================================================
-- 4. SECURITY DOMAIN VIEWS
-- =====================================================================================

-- v_user_list: Users with role info
CREATE OR REPLACE VIEW v_user_list AS
SELECT
    u.id,
    u.tenant_id,
    u.first_name,
    u.last_name,
    u.first_name || ' ' || u.last_name AS full_name,
    u.email,
    u.phone,
    u.status,
    u.last_login_at,
    u.login_attempts,
    u.locked_until,
    (SELECT COALESCE(string_agg(r.name, ', ' ORDER BY r.name), '') FROM user_roles ur JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = u.id) AS role_names,
    (SELECT COALESCE(json_agg(json_build_object('id', r.id, 'name', r.name, 'description', r.description) ORDER BY r.name), '[]'::json) FROM user_roles ur JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = u.id) AS roles,
    (SELECT COUNT(*) FROM user_roles ur WHERE ur.user_id = u.id) AS role_count,
    u.status = 'ACTIVE' AS is_active,
    u.locked_until IS NOT NULL AND u.locked_until > NOW() AS is_locked,
    u.deleted_at IS NOT NULL AS is_deleted,
    u.created_at,
    u.updated_at
FROM users u;

COMMENT ON VIEW v_user_list IS 'User listing with aggregated role information';

-- v_user_detail: Full user details with roles and permissions
CREATE OR REPLACE VIEW v_user_detail AS
SELECT
    u.id,
    u.tenant_id,
    u.first_name,
    u.last_name,
    u.first_name || ' ' || u.last_name AS full_name,
    u.email,
    u.phone,
    u.status,
    u.last_login_at,
    u.login_attempts,
    u.locked_until,
    u.password_changed_at,
    t.name AS tenant_name,
    t.subdomain AS tenant_subdomain,
    (
        SELECT COALESCE(json_agg(json_build_object(
            'id', r.id, 'name', r.name, 'description', r.description,
            'permissions', (SELECT COALESCE(json_agg(json_build_object('id', p.id, 'name', p.name, 'description', p.description, 'resource', p.resource, 'action', p.action) ORDER BY p.resource, p.action), '[]'::json) FROM role_permissions rp JOIN permissions p ON rp.permission_id = p.id WHERE rp.role_id = r.id)
        ) ORDER BY r.name), '[]'::json)
        FROM user_roles ur JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = u.id
    ) AS roles_with_permissions,
    (SELECT COALESCE(json_agg(DISTINCT p.name ORDER BY p.name), '[]'::json) FROM user_roles ur JOIN role_permissions rp ON ur.role_id = rp.role_id JOIN permissions p ON rp.permission_id = p.id WHERE ur.user_id = u.id) AS permission_names,
    u.created_at,
    u.updated_at,
    u.deleted_at
FROM users u
LEFT JOIN tenants t ON u.tenant_id = t.id;

COMMENT ON VIEW v_user_detail IS 'Complete user profile with roles and permissions hierarchy';

-- v_role_permissions: Roles with their permissions
CREATE OR REPLACE VIEW v_role_permissions AS
SELECT
    r.id,
    r.tenant_id,
    r.name,
    r.description,
    r.is_system_role,
    (SELECT COALESCE(json_agg(json_build_object('id', p.id, 'name', p.name, 'description', p.description, 'resource', p.resource, 'action', p.action) ORDER BY p.resource, p.action), '[]'::json) FROM role_permissions rp JOIN permissions p ON rp.permission_id = p.id WHERE rp.role_id = r.id) AS permissions,
    (SELECT COUNT(*) FROM role_permissions rp WHERE rp.role_id = r.id) AS permission_count,
    (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id = r.id) AS user_count,
    r.created_at,
    r.updated_at
FROM roles r
WHERE r.deleted_at IS NULL;

COMMENT ON VIEW v_role_permissions IS 'Role listing with embedded permissions as JSON';

-- v_tenant_summary: Tenant overview with counts
CREATE OR REPLACE VIEW v_tenant_summary AS
SELECT
    t.id,
    t.name,
    t.subdomain,
    t.email,
    t.phone,
    t.status,
    (SELECT COUNT(*) FROM users u WHERE u.tenant_id = t.id AND u.deleted_at IS NULL) AS user_count,
    (SELECT COUNT(*) FROM users u WHERE u.tenant_id = t.id AND u.deleted_at IS NULL AND u.status = 'ACTIVE') AS active_user_count,
    (SELECT COUNT(*) FROM patients p WHERE p.tenant_id = t.id AND p.deleted_at IS NULL) AS patient_count,
    (SELECT COUNT(*) FROM appointments a WHERE a.tenant_id = t.id AND a.deleted_at IS NULL) AS appointment_count,
    (SELECT COUNT(*) FROM appointments a WHERE a.tenant_id = t.id AND a.deleted_at IS NULL AND a.appointment_time::DATE = CURRENT_DATE) AS today_appointment_count,
    (SELECT COUNT(*) FROM billing b WHERE b.tenant_id = t.id AND b.deleted_at IS NULL) AS billing_count,
    (SELECT COALESCE(SUM(b.balance_amount), 0) FROM billing b WHERE b.tenant_id = t.id AND b.deleted_at IS NULL AND b.balance_amount > 0) AS outstanding_balance,
    t.created_at,
    t.updated_at
FROM tenants t
WHERE t.deleted_at IS NULL;

COMMENT ON VIEW v_tenant_summary IS 'Tenant summary with user, patient, and billing counts';

-- =====================================================================================
-- 5. DASHBOARD / ANALYTICS VIEWS
-- =====================================================================================

-- v_today_appointments: Today's appointments for dashboard
CREATE OR REPLACE VIEW v_today_appointments AS
SELECT
    a.id,
    a.tenant_id,
    a.patient_id,
    p.first_name || ' ' || p.last_name AS patient_name,
    p.phone AS patient_phone,
    a.doctor_id,
    d.first_name || ' ' || d.last_name AS doctor_name,
    a.appointment_time,
    TO_CHAR(a.appointment_time, 'HH24:MI') AS time_slot,
    a.duration_minutes,
    a.consultation_type,
    a.status,
    a.reason,
    CASE
        WHEN a.status = 'COMPLETED' THEN 'DONE'
        WHEN a.status = 'CANCELLED' THEN 'CANCELLED'
        WHEN a.status = 'IN_PROGRESS' THEN 'IN_PROGRESS'
        WHEN a.appointment_time < NOW() THEN 'OVERDUE'
        WHEN a.appointment_time <= NOW() + INTERVAL '30 minutes' THEN 'UPCOMING'
        ELSE 'SCHEDULED'
    END AS time_status,
    CASE a.status WHEN 'IN_PROGRESS' THEN 1 WHEN 'CONFIRMED' THEN 2 WHEN 'SCHEDULED' THEN 3 WHEN 'COMPLETED' THEN 4 WHEN 'CANCELLED' THEN 5 ELSE 6 END AS display_order
FROM appointments a
JOIN patients p ON a.patient_id = p.id AND p.deleted_at IS NULL
JOIN users d ON a.doctor_id = d.id AND d.deleted_at IS NULL
WHERE a.deleted_at IS NULL AND a.appointment_time::DATE = CURRENT_DATE;

COMMENT ON VIEW v_today_appointments IS 'Today''s appointments for dashboard display';

-- v_pending_lab_tests: Pending lab tests for dashboard
CREATE OR REPLACE VIEW v_pending_lab_tests AS
SELECT
    lt.id,
    lt.tenant_id,
    lt.patient_id,
    p.first_name || ' ' || p.last_name AS patient_name,
    p.phone AS patient_phone,
    lt.ordered_by,
    d.first_name || ' ' || d.last_name AS ordered_by_name,
    lt.test_name,
    lt.test_code,
    lt.status,
    lt.ordered_at,
    lt.sample_collected_at,
    CASE
        WHEN lt.status = 'SAMPLE_COLLECTED' THEN 1
        WHEN lt.status = 'IN_PROGRESS' THEN 2
        WHEN lt.status = 'ORDERED' AND lt.ordered_at < NOW() - INTERVAL '24 hours' THEN 3
        ELSE 4
    END AS priority,
    EXTRACT(EPOCH FROM (NOW() - lt.ordered_at)) / 3600 AS hours_since_ordered
FROM lab_tests lt
JOIN patients p ON lt.patient_id = p.id AND p.deleted_at IS NULL
JOIN users d ON lt.ordered_by = d.id AND d.deleted_at IS NULL
WHERE lt.deleted_at IS NULL AND lt.status IN ('ORDERED', 'SAMPLE_COLLECTED', 'IN_PROGRESS');

COMMENT ON VIEW v_pending_lab_tests IS 'Pending lab tests queue with priority ordering';

-- v_overdue_payments: Overdue billing records
CREATE OR REPLACE VIEW v_overdue_payments AS
SELECT
    b.id,
    b.tenant_id,
    b.patient_id,
    p.first_name || ' ' || p.last_name AS patient_name,
    p.phone AS patient_phone,
    p.email AS patient_email,
    b.invoice_number,
    b.invoice_date,
    b.total_amount,
    b.paid_amount,
    b.balance_amount,
    b.payment_status,
    CURRENT_DATE - b.invoice_date AS days_overdue,
    CASE
        WHEN CURRENT_DATE - b.invoice_date <= 30 THEN '0-30 days'
        WHEN CURRENT_DATE - b.invoice_date <= 60 THEN '31-60 days'
        WHEN CURRENT_DATE - b.invoice_date <= 90 THEN '61-90 days'
        ELSE '90+ days'
    END AS aging_bucket,
    (CURRENT_DATE - b.invoice_date) * b.balance_amount AS priority_score
FROM billing b
JOIN patients p ON b.patient_id = p.id AND p.deleted_at IS NULL
WHERE b.deleted_at IS NULL AND b.balance_amount > 0 AND b.invoice_date < CURRENT_DATE - INTERVAL '30 days';

COMMENT ON VIEW v_overdue_payments IS 'Overdue payment records with aging buckets';

-- =====================================================================================
-- CREATE SUPPORTING INDEXES FOR VIEW PERFORMANCE
-- =====================================================================================

CREATE INDEX IF NOT EXISTS idx_medical_records_patient ON medical_records(patient_id, tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_prescriptions_patient ON prescriptions(patient_id, tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_lab_tests_status ON lab_tests(tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_billing_status ON billing(tenant_id, payment_status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_inventory_stock ON inventory(tenant_id, current_stock, minimum_stock) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_notifications_user_status ON notifications(user_id, tenant_id, status);

-- =====================================================================================
-- END OF MIGRATION
-- =====================================================================================
