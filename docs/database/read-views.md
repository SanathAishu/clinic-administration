# Database Read Views - CQRS Pattern

## Overview

The Clinic Management System implements a CQRS (Command Query Responsibility Segregation) pattern using database views for optimized READ operations.

**Architecture:**
- **Commands (CREATE, UPDATE, DELETE)**: Handled by JPA entities with relationships at the application level
- **Queries (READ)**: Offloaded to PostgreSQL views to leverage DB engine optimization

**Benefits:**
- Pre-joined data eliminates N+1 query problems
- Database engine optimizes complex JOINs efficiently
- Simpler application code (no complex JPQL/HQL for reads)
- Better separation of concerns
- Improved query performance through DB-level optimization

---

## Migration

**Migration File:** `V6__create_read_views.sql`

**Location:** `clinic-migrations/src/main/resources/db/migration/`

---

## Views by Domain

### 1. Patient Domain (5 views)

| View | Purpose | Key Features |
|------|---------|--------------|
| `v_patient_list` | Patient listing | Computed full_name, age, status flags |
| `v_patient_detail` | Full patient profile | Latest vitals via LATERAL JOIN, summary counts |
| `v_patient_appointments` | Patient appointment history | Doctor details, medical record link |
| `v_patient_medical_history` | Medical record history | Diagnosis/prescription/lab test counts |
| `v_patient_billing_history` | Billing history | Payment status, overdue flags |

**Example Query - Patient List:**
```sql
SELECT * FROM v_patient_list
WHERE tenant_id = :tenantId AND is_active = true
ORDER BY full_name;
```

**Example Query - Patient Detail:**
```sql
SELECT * FROM v_patient_detail
WHERE id = :patientId AND tenant_id = :tenantId;
-- Returns: full profile + latest vitals + summary counts
```

### 2. Clinical Domain (7 views)

| View | Purpose | Key Features |
|------|---------|--------------|
| `v_appointment_list` | Appointment scheduling | Patient/doctor info, time_category (TODAY/UPCOMING/PAST) |
| `v_appointment_detail` | Full appointment details | Medical record, billing, cancellation info |
| `v_medical_record_detail` | Medical record | Diagnoses and prescriptions as JSON arrays |
| `v_prescription_list` | Prescription listing | Items as embedded JSON array |
| `v_lab_test_list` | Lab test listing | Result summary, abnormal flags |
| `v_lab_test_detail` | Full lab test | All results as JSON array |
| `v_diagnosis_list` | Diagnosis listing | Patient and doctor info |

**Example Query - Today's Schedule:**
```sql
SELECT * FROM v_appointment_list
WHERE tenant_id = :tenantId AND time_category = 'TODAY'
ORDER BY appointment_time;
```

**Example Query - Medical Record with Diagnoses:**
```sql
SELECT id, patient_name, chief_complaint, diagnoses::text
FROM v_medical_record_detail
WHERE id = :recordId;
-- diagnoses column contains JSON array
```

### 3. Operational Domain (7 views)

| View | Purpose | Key Features |
|------|---------|--------------|
| `v_billing_list` | Billing records | Payment status, aging calculations, days_overdue |
| `v_inventory_list` | Inventory items | Stock status (LOW_STOCK, OUT_OF_STOCK, EXPIRING_SOON) |
| `v_inventory_low_stock` | Low stock alerts | Priority ordering, suggested_reorder_quantity |
| `v_inventory_transactions` | Transaction history | Item info, performed_by_name, direction (IN/OUT) |
| `v_notification_list` | Notifications | User info, status flags |
| `v_staff_schedule_list` | Staff schedules | Day names, hours_scheduled, schedule_status |
| `v_consent_records` | Consent records | Patient info, validity status |

**Example Query - Low Stock Alerts:**
```sql
SELECT * FROM v_inventory_low_stock
WHERE tenant_id = :tenantId
ORDER BY priority, item_name;
```

**Example Query - Overdue Bills:**
```sql
SELECT * FROM v_billing_list
WHERE tenant_id = :tenantId AND billing_status = 'OVERDUE'
ORDER BY days_overdue DESC;
```

### 4. Security Domain (4 views)

| View | Purpose | Key Features |
|------|---------|--------------|
| `v_user_list` | User listing | Role names, roles as JSON, status flags |
| `v_user_detail` | Full user profile | Roles with embedded permissions as JSON |
| `v_role_permissions` | Role management | Permissions as JSON, user_count |
| `v_tenant_summary` | Tenant overview | User/patient/appointment/billing counts |

**Example Query - Users with Roles:**
```sql
SELECT id, full_name, email, role_names, is_active
FROM v_user_list
WHERE tenant_id = :tenantId AND is_deleted = false
ORDER BY full_name;
```

### 5. Dashboard Views (3 views)

| View | Purpose | Key Features |
|------|---------|--------------|
| `v_today_appointments` | Today's schedule | time_status (OVERDUE, UPCOMING, IN_PROGRESS), display_order |
| `v_pending_lab_tests` | Lab test queue | Priority ordering, hours_since_ordered |
| `v_overdue_payments` | Overdue payments | aging_bucket (0-30, 31-60, etc.), priority_score |

**Example Query - Dashboard Overview:**
```sql
-- Today's appointments by priority
SELECT * FROM v_today_appointments
WHERE tenant_id = :tenantId
ORDER BY display_order, appointment_time;

-- Pending lab tests by urgency
SELECT * FROM v_pending_lab_tests
WHERE tenant_id = :tenantId
ORDER BY priority, ordered_at;

-- Overdue payments by severity
SELECT * FROM v_overdue_payments
WHERE tenant_id = :tenantId
ORDER BY priority_score DESC;
```

---

## JSON Aggregation Pattern

Several views embed related data as JSON arrays for efficient single-query retrieval:

```sql
-- v_medical_record_detail.diagnoses
SELECT
    id,
    patient_name,
    diagnoses::text  -- Returns: [{"id": "...", "icd10_code": "...", "diagnosis_name": "..."}]
FROM v_medical_record_detail;

-- v_prescription_list.items
SELECT
    id,
    patient_name,
    items::text  -- Returns: [{"medication_name": "...", "dosage": "...", "frequency": "..."}]
FROM v_prescription_list;

-- v_user_list.roles
SELECT
    id,
    full_name,
    roles::text  -- Returns: [{"id": "...", "name": "...", "description": "..."}]
FROM v_user_list;
```

---

## LATERAL JOIN Pattern

Used for "latest" record retrieval (e.g., latest vitals):

```sql
-- v_patient_detail uses LATERAL JOIN for latest vitals
SELECT
    p.*,
    lv.temperature_celsius,
    lv.blood_pressure
FROM patients p
LEFT JOIN LATERAL (
    SELECT * FROM vitals v
    WHERE v.patient_id = p.id
    ORDER BY v.recorded_at DESC
    LIMIT 1
) lv ON true;
```

---

## Computed Columns

Views provide computed columns for common use cases:

| Column | Source | Example |
|--------|--------|---------|
| `full_name` | `first_name + middle_name + last_name` | "John A. Doe" |
| `age` | `EXTRACT(YEAR FROM AGE(date_of_birth))` | 45 |
| `blood_pressure` | `systolic_bp + '/' + diastolic_bp` | "120/80" |
| `is_active` | `deleted_at IS NULL` | true/false |
| `is_overdue` | `appointment_time < NOW() AND status = 'SCHEDULED'` | true/false |
| `stock_status` | Case logic for stock levels | "LOW_STOCK" |
| `days_overdue` | `CURRENT_DATE - invoice_date` | 45 |
| `aging_bucket` | Case logic for payment aging | "31-60 days" |

---

## Performance Indexes

Supporting indexes created for view performance:

```sql
CREATE INDEX idx_medical_records_patient ON medical_records(patient_id, tenant_id);
CREATE INDEX idx_prescriptions_patient ON prescriptions(patient_id, tenant_id);
CREATE INDEX idx_lab_tests_status ON lab_tests(tenant_id, status);
CREATE INDEX idx_billing_status ON billing(tenant_id, payment_status);
CREATE INDEX idx_inventory_stock ON inventory(tenant_id, current_stock, minimum_stock);
CREATE INDEX idx_notifications_user_status ON notifications(user_id, tenant_id, status);
```

---

## Usage in Spring Data JPA

### Option 1: Native Queries

```java
@Repository
public interface PatientViewRepository extends JpaRepository<Patient, UUID> {

    @Query(value = "SELECT * FROM v_patient_list WHERE tenant_id = ?1 AND is_active = true",
           nativeQuery = true)
    List<Object[]> findActivePatients(UUID tenantId);

    @Query(value = "SELECT * FROM v_patient_detail WHERE id = ?1 AND tenant_id = ?2",
           nativeQuery = true)
    Object[] findPatientDetail(UUID patientId, UUID tenantId);
}
```

### Option 2: View-Mapped DTOs (Recommended)

```java
// Create immutable view-mapped entity (read-only)
@Entity
@Immutable
@Table(name = "v_patient_list")
public class PatientListView {
    @Id
    private UUID id;
    private UUID tenantId;
    private String fullName;
    private Integer age;
    private String gender;
    private String phone;
    private Boolean isActive;
    // ... getters only
}

@Repository
public interface PatientListViewRepository extends JpaRepository<PatientListView, UUID> {
    List<PatientListView> findByTenantIdAndIsActiveTrue(UUID tenantId);
}
```

### Option 3: DTO Projections

```java
public interface PatientSummary {
    UUID getId();
    String getFullName();
    Integer getAge();
    String getPhone();
    Boolean getIsActive();
}

@Repository
public interface PatientViewRepository extends JpaRepository<Patient, UUID> {
    @Query(value = "SELECT id, full_name as fullName, age, phone, is_active as isActive " +
                   "FROM v_patient_list WHERE tenant_id = ?1 AND is_active = true",
           nativeQuery = true)
    List<PatientSummary> findActivePatientSummaries(UUID tenantId);
}
```

---

## Multi-Tenancy

All views include `tenant_id` for RLS compatibility:

```sql
-- All queries must filter by tenant_id
SELECT * FROM v_patient_list WHERE tenant_id = :currentTenantId;

-- Views automatically filter soft-deleted records
-- (WHERE deleted_at IS NULL is built into most views)
```

---

## View Summary

| Category | Views | Description |
|----------|-------|-------------|
| Patient | 5 | Patient profiles and history |
| Clinical | 7 | Appointments, records, prescriptions, labs |
| Operations | 7 | Billing, inventory, notifications, schedules |
| Security | 4 | Users, roles, permissions, tenants |
| Dashboard | 3 | Today's appointments, pending items, overdue payments |
| **Total** | **26** | Complete CQRS read layer |

---

## Best Practices

1. **Always filter by tenant_id** - Required for multi-tenancy isolation
2. **Use pagination** - Views can return large result sets
3. **Leverage computed columns** - Avoid recalculating in application code
4. **Use JSON columns wisely** - Parse JSON in application when needed
5. **Create view-specific DTOs** - Don't reuse entity classes for view data
6. **Add indexes** - Create indexes for frequently filtered columns

---

## References

- [PostgreSQL Views](https://www.postgresql.org/docs/16/sql-createview.html)
- [PostgreSQL JSON Functions](https://www.postgresql.org/docs/16/functions-json.html)
- [LATERAL Joins](https://www.postgresql.org/docs/16/queries-table-expressions.html#QUERIES-LATERAL)
- [Spring Data JPA Projections](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#projections)
