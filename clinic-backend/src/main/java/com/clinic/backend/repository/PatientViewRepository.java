package com.clinic.backend.repository;

import com.clinic.common.dto.view.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for read-only operations using database views (CQRS pattern).
 * All queries use native SQL to directly access database views.
 *
 * Views provide:
 * - Pre-computed fields (full_name, age, status flags)
 * - Pre-joined data (eliminates N+1 problems)
 * - Database-level optimizations
 */
@Repository
public interface PatientViewRepository extends JpaRepository<com.clinic.common.entity.patient.Patient, UUID> {

    // ============================
    // Patient List View Queries
    // ============================

    /**
     * Get paginated list of active patients for a tenant.
     * Uses v_patient_list view.
     */
    @Query(value = """
        SELECT id, tenant_id, first_name, middle_name, last_name, full_name,
               date_of_birth, age, gender, phone, email, blood_group, abha_id,
               created_at, updated_at, status, is_active
        FROM v_patient_list
        WHERE tenant_id = :tenantId AND is_active = true
        ORDER BY full_name
        """,
        countQuery = """
        SELECT COUNT(*) FROM v_patient_list
        WHERE tenant_id = :tenantId AND is_active = true
        """,
        nativeQuery = true)
    Page<Object[]> findAllPatientsForTenant(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Search patients by name, phone, or email.
     * Uses v_patient_list view.
     */
    @Query(value = """
        SELECT id, tenant_id, first_name, middle_name, last_name, full_name,
               date_of_birth, age, gender, phone, email, blood_group, abha_id,
               created_at, updated_at, status, is_active
        FROM v_patient_list
        WHERE tenant_id = :tenantId AND is_active = true
        AND (LOWER(full_name) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(email) LIKE LOWER(CONCAT('%', :search, '%'))
             OR phone LIKE CONCAT('%', :search, '%')
             OR abha_id LIKE CONCAT('%', :search, '%'))
        ORDER BY full_name
        """,
        countQuery = """
        SELECT COUNT(*) FROM v_patient_list
        WHERE tenant_id = :tenantId AND is_active = true
        AND (LOWER(full_name) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(email) LIKE LOWER(CONCAT('%', :search, '%'))
             OR phone LIKE CONCAT('%', :search, '%')
             OR abha_id LIKE CONCAT('%', :search, '%'))
        """,
        nativeQuery = true)
    Page<Object[]> searchPatients(
        @Param("tenantId") UUID tenantId,
        @Param("search") String search,
        Pageable pageable
    );

    // ============================
    // Patient Detail View Queries
    // ============================

    /**
     * Get full patient details including latest vitals and counts.
     * Uses v_patient_detail view.
     */
    @Query(value = """
        SELECT id, tenant_id, first_name, middle_name, last_name, full_name,
               date_of_birth, age, gender, phone, email,
               address_line1, address_line2, city, state, pincode,
               blood_group, abha_id, abha_number, marital_status, occupation,
               emergency_contact_name, emergency_contact_phone, emergency_contact_relation,
               allergies, chronic_conditions,
               created_at, updated_at, deleted_at,
               latest_vital_id, latest_vital_recorded_at,
               temperature_celsius, pulse_bpm, systolic_bp, diastolic_bp,
               respiratory_rate, oxygen_saturation, weight_kg, height_cm, bmi, blood_pressure,
               total_appointments, total_medical_records, total_prescriptions, total_lab_tests, outstanding_balance
        FROM v_patient_detail
        WHERE id = :patientId AND tenant_id = :tenantId AND deleted_at IS NULL
        """,
        nativeQuery = true)
    Object[] findPatientDetailById(@Param("patientId") UUID patientId, @Param("tenantId") UUID tenantId);

    // ============================
    // Patient Appointments View Queries
    // ============================

    /**
     * Get patient's appointment history.
     * Uses v_patient_appointments view.
     */
    @Query(value = """
        SELECT id, tenant_id, patient_id, patient_name, patient_phone,
               doctor_id, doctor_name, doctor_email,
               appointment_time, appointment_date, appointment_time_only, duration_minutes, end_time,
               consultation_type, status, reason, notes,
               confirmed_at, started_at, completed_at, cancelled_at, cancellation_reason,
               created_at, updated_at,
               medical_record_id, chief_complaint,
               is_overdue, is_today
        FROM v_patient_appointments
        WHERE patient_id = :patientId AND tenant_id = :tenantId
        ORDER BY appointment_time DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM v_patient_appointments
        WHERE patient_id = :patientId AND tenant_id = :tenantId
        """,
        nativeQuery = true)
    Page<Object[]> findPatientAppointments(
        @Param("patientId") UUID patientId,
        @Param("tenantId") UUID tenantId,
        Pageable pageable
    );

    // ============================
    // Patient Medical History View Queries
    // ============================

    /**
     * Get patient's medical history.
     * Uses v_patient_medical_history view.
     */
    @Query(value = """
        SELECT id, tenant_id, patient_id, patient_name,
               doctor_id, doctor_name,
               record_date, chief_complaint, history_present_illness, examination_findings,
               clinical_notes, treatment_plan, follow_up_instructions, follow_up_date,
               appointment_id, appointment_time, consultation_type,
               diagnosis_count, prescription_count, lab_test_count,
               created_at, updated_at
        FROM v_patient_medical_history
        WHERE patient_id = :patientId AND tenant_id = :tenantId
        ORDER BY record_date DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM v_patient_medical_history
        WHERE patient_id = :patientId AND tenant_id = :tenantId
        """,
        nativeQuery = true)
    Page<Object[]> findPatientMedicalHistory(
        @Param("patientId") UUID patientId,
        @Param("tenantId") UUID tenantId,
        Pageable pageable
    );

    // ============================
    // Patient Billing History View Queries
    // ============================

    /**
     * Get patient's billing history.
     * Uses v_patient_billing_history view.
     */
    @Query(value = """
        SELECT id, tenant_id, patient_id, patient_name, patient_phone,
               invoice_number, invoice_date,
               subtotal, discount_amount, tax_amount, total_amount, paid_amount, balance_amount,
               payment_status, payment_method, payment_date, payment_reference,
               line_items,
               appointment_id, appointment_time, consultation_type,
               doctor_id, doctor_name,
               is_overdue, has_balance,
               created_at, updated_at
        FROM v_patient_billing_history
        WHERE patient_id = :patientId AND tenant_id = :tenantId
        ORDER BY invoice_date DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM v_patient_billing_history
        WHERE patient_id = :patientId AND tenant_id = :tenantId
        """,
        nativeQuery = true)
    Page<Object[]> findPatientBillingHistory(
        @Param("patientId") UUID patientId,
        @Param("tenantId") UUID tenantId,
        Pageable pageable
    );
}
