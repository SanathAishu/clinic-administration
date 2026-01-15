# Database Migrations Guide

## Overview

The Clinic Management System uses [Flyway](https://flywaydb.org/) for database version control and migrations. This guide covers how migrations work, how to create new ones, and best practices.

---

## Migration Files

### Location

```
clinic-migrations/src/main/resources/db/migration/
├── V1__create_foundation_tables.sql      # Tenants, users, roles, permissions, audit_logs
├── V2__create_identity_access_tables.sql # Sessions, user_roles, role_permissions
├── V3__create_patient_care_tables.sql    # Patients, appointments, medical_records, etc.
├── V4__create_operations_tables.sql      # Billing, inventory, notifications, etc.
├── V5__create_materialized_views_phase1.sql # 3 materialized views
└── V6__create_read_views.sql             # 26 CQRS read views
```

### Naming Convention

Flyway requires a specific naming pattern:

```
V{version}__{description}.sql
```

**Examples:**
- `V1__create_foundation_tables.sql` - Version 1, foundation tables
- `V3__create_patient_care_tables.sql` - Version 3, patient care tables
- `V6__create_read_views.sql` - Version 6, CQRS read views

**Rules:**
- Prefix: `V` (uppercase)
- Version: Integer or decimal (e.g., `1`, `2`, `2.1`, `3`)
- Separator: `__` (double underscore)
- Description: Snake_case or lowercase with underscores
- Extension: `.sql`

**Invalid Names:**
- ❌ `v1__create_schema.sql` (lowercase v)
- ❌ `V1_create_schema.sql` (single underscore)
- ❌ `V1__Create Schema.sql` (spaces)
- ❌ `create_schema.sql` (no version)

---

## Current Migration State

### Applied Migrations

Check which migrations have been applied:

```bash
# Via Docker
docker exec clinic-postgres psql -U clinic_user -d clinic -c "
SELECT installed_rank, version, description, type, script, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;"
```

**Expected Output:**
```
 installed_rank | version | description                        | type | script                                        | installed_on        | success
----------------+---------+------------------------------------+------+-----------------------------------------------+---------------------+---------
              1 | 1       | create foundation tables           | SQL  | V1__create_foundation_tables.sql              | 2026-01-15 10:00:00 | t
              2 | 2       | create identity access tables      | SQL  | V2__create_identity_access_tables.sql         | 2026-01-15 10:00:01 | t
              3 | 3       | create patient care tables         | SQL  | V3__create_patient_care_tables.sql            | 2026-01-15 10:00:02 | t
              4 | 4       | create operations tables           | SQL  | V4__create_operations_tables.sql              | 2026-01-15 10:00:03 | t
              5 | 5       | create materialized views phase1   | SQL  | V5__create_materialized_views_phase1.sql      | 2026-01-15 15:49:24 | t
              6 | 6       | create read views                  | SQL  | V6__create_read_views.sql                     | 2026-01-16 01:30:00 | t
```

---

## Running Migrations

### Automatic Migration (Application Startup)

Migrations run automatically when the application starts:

```bash
cd clinic-backend
./gradlew bootRun

# Output will show:
# Flyway: Migrating schema "public" to version 5 - create materialized views phase1
# Flyway: Successfully applied 1 migration
```

**Configuration (`application.yml`):**
```yaml
spring:
  flyway:
    enabled: true                    # Enable Flyway
    baseline-on-migrate: true        # Allow migrations on non-empty DB
    locations: classpath:db/migration # Migration file location
    schemas: public                   # Target schema
    validate-on-migrate: true         # Validate checksums
```

### Manual Migration

Run migrations manually using Gradle:

```bash
cd clinic-backend
./gradlew flywayMigrate

# Output:
# Flyway: Migrating schema "public" to version 5
# Flyway: Successfully applied 1 migration (execution time 00:00.324s)
```

### Other Flyway Commands

```bash
# Show migration status
./gradlew flywayInfo

# Validate applied migrations against available migrations
./gradlew flywayValidate

# Repair failed migration (removes failed entry from history)
./gradlew flywayRepair

# Clean database (⚠️ DESTRUCTIVE - drops all objects)
./gradlew flywayClean

# Baseline existing database (mark current state as version 1)
./gradlew flywayBaseline
```

---

## Creating New Migrations

### Step 1: Determine Version Number

Check the latest migration version:

```bash
ls -1 clinic-migrations/src/main/resources/db/migration/ | tail -1

# Output: V5__create_materialized_views_phase1.sql
# Next version: V6
```

### Step 2: Create Migration File

```bash
cd clinic-migrations/src/main/resources/db/migration/

# Create new file
touch V6__add_patient_allergies_table.sql
```

### Step 3: Write Migration SQL

**Example: Adding a new table**

```sql
-- V6__add_patient_allergies_table.sql

-- =====================================================================================
-- Migration: V6__add_patient_allergies_table.sql
-- Description: Add patient allergies tracking
-- Author: Your Name
-- Date: 2026-01-16
-- =====================================================================================

-- Create patient_allergies table
CREATE TABLE patient_allergies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,

    -- Allergy details
    allergen VARCHAR(255) NOT NULL,
    severity VARCHAR(50) NOT NULL CHECK (severity IN ('MILD', 'MODERATE', 'SEVERE', 'ANAPHYLAXIS')),
    reaction TEXT,
    notes TEXT,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),

    -- Soft delete
    deleted_at TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_patient_allergies_patient ON patient_allergies(patient_id, tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_patient_allergies_tenant ON patient_allergies(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_patient_allergies_severity ON patient_allergies(tenant_id, severity) WHERE deleted_at IS NULL;

-- Enable Row-Level Security
ALTER TABLE patient_allergies ENABLE ROW LEVEL SECURITY;

-- Create RLS policy
CREATE POLICY tenant_isolation_policy ON patient_allergies
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID);

-- Add comments
COMMENT ON TABLE patient_allergies IS 'Patient allergy and adverse reaction records';
COMMENT ON COLUMN patient_allergies.severity IS 'Allergy severity: MILD, MODERATE, SEVERE, ANAPHYLAXIS';

-- =====================================================================================
-- END OF MIGRATION
-- =====================================================================================
```

### Step 4: Test Migration

**Test on development database:**

```bash
# Create test database
docker exec clinic-postgres psql -U clinic_user -c "CREATE DATABASE clinic_test;"

# Apply migrations to test database
./gradlew flywayMigrate -Dflyway.url=jdbc:postgresql://localhost:5432/clinic_test

# Verify migration
docker exec clinic-postgres psql -U clinic_user -d clinic_test -c "\d+ patient_allergies"

# Drop test database after verification
docker exec clinic-postgres psql -U clinic_user -c "DROP DATABASE clinic_test;"
```

**Test with application:**

```bash
# Run application (will auto-migrate)
./gradlew bootRun

# Check logs for migration success
```

### Step 5: Commit Migration

```bash
git add clinic-migrations/src/main/resources/db/migration/V6__add_patient_allergies_table.sql
git commit -m "feat(database): add patient allergies table"
```

---

## Migration Best Practices

### 1. Idempotent Operations

Use `IF EXISTS` / `IF NOT EXISTS` where possible:

```sql
-- ✓ GOOD - Idempotent
CREATE TABLE IF NOT EXISTS patients (...);
DROP TABLE IF EXISTS old_table;
CREATE INDEX IF NOT EXISTS idx_patients_email ON patients(email);

-- ✗ BAD - Will fail if already exists
CREATE TABLE patients (...);
```

### 2. Never Modify Applied Migrations

**Rule:** Once a migration is applied (committed to version control and run), NEVER modify it.

**Why?** Flyway tracks checksums. Modified migrations will cause validation errors.

**Instead:** Create a new migration to make changes.

```sql
-- ✗ WRONG - Modifying V3__create_core_tables.sql after it's applied

-- ✓ CORRECT - Create new migration
-- V7__add_patient_email_column.sql
ALTER TABLE patients ADD COLUMN email VARCHAR(255);
```

### 3. One Purpose Per Migration

Keep migrations focused:

```sql
-- ✓ GOOD - Single purpose
-- V6__add_patient_allergies_table.sql
-- V7__add_medication_formulary_table.sql
-- V8__add_patient_email_column.sql

-- ✗ BAD - Multiple unrelated changes
-- V6__various_updates.sql (adds allergies, medications, changes email, etc.)
```

### 4. Use Transactions

Wrap migrations in transactions (default for Flyway):

```sql
-- Flyway automatically wraps in transaction, but you can be explicit:
BEGIN;

-- Migration statements here
CREATE TABLE ...;
CREATE INDEX ...;

COMMIT;
```

**Note:** Some statements cannot be in transactions (e.g., `CREATE INDEX CONCURRENTLY`). In such cases, add Flyway configuration:

```sql
-- Add this comment at the top of migration
-- flyway:executeInTransaction=false

CREATE INDEX CONCURRENTLY idx_patients_email ON patients(email);
```

### 5. Add Rollback Comments

While Flyway doesn't support automatic rollback, document how to undo changes:

```sql
-- =====================================================================================
-- Migration: V6__add_patient_allergies_table.sql
-- =====================================================================================

CREATE TABLE patient_allergies (...);

-- =====================================================================================
-- ROLLBACK (if needed)
-- =====================================================================================
-- To manually rollback this migration:
-- DROP TABLE IF EXISTS patient_allergies CASCADE;
-- DELETE FROM flyway_schema_history WHERE version = '6';
-- =====================================================================================
```

### 6. Test Migrations Thoroughly

**Before committing:**
1. Test on clean database
2. Test on database with existing data
3. Verify indexes were created
4. Check RLS policies work
5. Test rollback procedure

### 7. Document Complex Migrations

Add detailed comments for complex migrations:

```sql
-- =====================================================================================
-- Migration: V10__partition_audit_logs_2026.sql
-- Description: Create monthly partitions for audit_logs table for year 2026
-- Author: Your Name
-- Date: 2026-01-16
--
-- This migration creates 12 monthly partitions for the audit_logs table.
-- Each partition covers one calendar month.
--
-- Performance Impact:
-- - Queries with date filters will scan only relevant partitions
-- - Expected 80% reduction in query time for date-range queries
--
-- Maintenance:
-- - New partitions should be created monthly (can be automated)
-- - Old partitions can be dropped after retention period (e.g., 7 years)
-- =====================================================================================
```

### 8. Handle Data Migrations Carefully

When migrating data, ensure idempotency:

```sql
-- V11__migrate_patient_email_format.sql

-- Update email format (ensure idempotent)
UPDATE patients
SET email = LOWER(TRIM(email))
WHERE email IS NOT NULL
  AND email != LOWER(TRIM(email));  -- Only update if different

-- Update records could be 0 if already migrated
-- This is fine - migration is idempotent
```

### 9. Use Descriptive Names

```sql
-- ✓ GOOD - Clear and descriptive
V6__add_patient_allergies_table.sql
V7__create_appointment_reminders_view.sql
V8__add_email_index_to_users.sql
V9__migrate_old_billing_format.sql

-- ✗ BAD - Vague or unclear
V6__updates.sql
V7__changes.sql
V8__fix.sql
```

### 10. Version Numbering Strategy

**Simple Sequential (Recommended):**
```
V1, V2, V3, V4, V5, ...
```

**Decimal for Hotfixes:**
```
V1, V2, V3, V3.1 (hotfix), V4, V5, ...
```

**Date-Based (Alternative):**
```
V2026_01_15_001__create_base_schema.sql
V2026_01_15_002__create_enums.sql
```

---

## Common Migration Patterns

### Adding a Column

```sql
-- V6__add_patient_middle_name.sql
ALTER TABLE patients ADD COLUMN middle_name VARCHAR(100);

-- With default value
ALTER TABLE patients ADD COLUMN is_vip BOOLEAN NOT NULL DEFAULT false;

-- Rollback:
-- ALTER TABLE patients DROP COLUMN middle_name;
```

### Adding an Index

```sql
-- V7__add_email_index.sql
CREATE INDEX idx_patients_email ON patients(email) WHERE deleted_at IS NULL;

-- For large tables, use CONCURRENTLY (requires executeInTransaction=false)
-- flyway:executeInTransaction=false
CREATE INDEX CONCURRENTLY idx_patients_email ON patients(email);

-- Rollback:
-- DROP INDEX IF EXISTS idx_patients_email;
```

### Adding a Foreign Key

```sql
-- V8__add_patient_primary_doctor_fk.sql
ALTER TABLE patients
ADD COLUMN primary_doctor_id UUID REFERENCES users(id);

CREATE INDEX idx_patients_primary_doctor ON patients(primary_doctor_id);

-- Rollback:
-- ALTER TABLE patients DROP COLUMN primary_doctor_id;
```

### Creating an Enum

```sql
-- V9__create_appointment_type_enum.sql
CREATE TYPE appointment_type_enum AS ENUM (
    'CONSULTATION',
    'FOLLOW_UP',
    'EMERGENCY',
    'ROUTINE_CHECKUP'
);

ALTER TABLE appointments
ADD COLUMN appointment_type appointment_type_enum;

-- Rollback:
-- ALTER TABLE appointments DROP COLUMN appointment_type;
-- DROP TYPE appointment_type_enum;
```

### Data Migration

```sql
-- V10__migrate_phone_format.sql

-- Standardize phone format: remove spaces and dashes
UPDATE patients
SET phone = REGEXP_REPLACE(phone, '[^0-9+]', '', 'g')
WHERE phone IS NOT NULL
  AND phone ~ '[^0-9+]';  -- Only if contains non-digit characters

-- Add validation
ALTER TABLE patients ADD CONSTRAINT chk_phone_format
    CHECK (phone IS NULL OR phone ~ '^\\+?[0-9]{10,15}$');

-- Rollback:
-- ALTER TABLE patients DROP CONSTRAINT chk_phone_format;
-- (Phone data cannot be easily reverted)
```

---

## Troubleshooting

### Issue 1: Migration Failed

**Error:**
```
ERROR: Flyway migration failed
  SQL State  : 42P01
  Error Code : 0
  Message    : relation "nonexistent_table" does not exist
```

**Solutions:**

1. **Check migration SQL:**
   ```bash
   cat clinic-migrations/src/main/resources/db/migration/V6__failing_migration.sql
   ```

2. **Fix the SQL and mark as repaired:**
   ```bash
   ./gradlew flywayRepair
   ```

3. **Re-run migration:**
   ```bash
   ./gradlew flywayMigrate
   ```

### Issue 2: Checksum Mismatch

**Error:**
```
Validate failed: Migrations have failed validation
Migration checksum mismatch for migration version 3
```

**Cause:** Migration file was modified after being applied.

**Solutions:**

**Option A: Repair (if changes were accidental):**
```bash
# Revert file to original version
git checkout HEAD -- clinic-migrations/src/main/resources/db/migration/V3__create_core_tables.sql

# Validate
./gradlew flywayValidate
```

**Option B: Update checksum (if changes were intentional but safe):**
```bash
./gradlew flywayRepair
```

**Option C: Create new migration (recommended):**
```sql
-- V7__fix_previous_migration_issue.sql
-- Correct the issue with new migration instead of modifying old one
```

### Issue 3: Migration Out of Order

**Error:**
```
Detected resolved migration not applied to database: 5
```

**Cause:** Added migration with version number lower than latest applied.

**Solution:**
```bash
# Rename migration with higher version number
mv V5__new_migration.sql V8__new_migration.sql

# Or allow out-of-order (not recommended for production)
# In application.yml:
# flyway.out-of-order: true
```

### Issue 4: Baseline Existing Database

**Scenario:** Applying Flyway to existing database

**Solution:**
```bash
# Mark current state as version 1 (baseline)
./gradlew flywayBaseline

# Then apply new migrations
./gradlew flywayMigrate
```

---

## References

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Flyway SQL Migrations](https://flywaydb.org/documentation/concepts/migrations#sql-based-migrations)
- [PostgreSQL ALTER TABLE](https://www.postgresql.org/docs/16/sql-altertable.html)
- [PostgreSQL CREATE INDEX](https://www.postgresql.org/docs/16/sql-createindex.html)

---

## Next Steps

1. Review existing migrations: `clinic-migrations/src/main/resources/db/migration/`
2. Understand database schema: `docs/database/setup.md`
3. Learn materialized views: `docs/database/materialized-views/`
4. Explore architecture: `docs/architecture/`
