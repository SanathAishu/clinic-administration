# Database Setup Guide

## Overview

The Clinic Management System uses PostgreSQL 16 with advanced features including:
- Row-Level Security (RLS) for multi-tenancy
- Materialized views for performance
- JSONB for flexible metadata
- Partitioning for audit logs
- Custom enum types

---

## Prerequisites

- Docker and Docker Compose
- PostgreSQL 16 client tools (optional, for direct access)
- Java 21 (for application)

---

## Quick Start

### Option 1: Using Docker (Recommended)

**1. Start PostgreSQL Container**

```bash
docker run -d \
  --name clinic-postgres \
  -e POSTGRES_USER=clinic_user \
  -e POSTGRES_PASSWORD=your_secure_password \
  -e POSTGRES_DB=clinic \
  -p 5432:5432 \
  postgres:16
```

**Current Running Container:**
- Container ID: `2afeec983133`
- Container Name: `clinic-postgres`
- Database: `clinic`
- User: `clinic_user`
- Port: `5432`

**2. Verify Container is Running**

```bash
docker ps | grep clinic-postgres

# Check PostgreSQL version
docker exec clinic-postgres psql -U clinic_user -d clinic -c "SELECT version();"
```

**3. Run Migrations**

Migrations run automatically when the application starts (via Flyway):

```bash
cd clinic-backend
./gradlew bootRun
```

Or manually apply migrations:

```bash
cd clinic-backend
./gradlew flywayMigrate
```

---

## Database Configuration

### Connection Details

**JDBC URL:**
```
jdbc:postgresql://localhost:5432/clinic
```

**Application Properties (`application.yml`):**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/clinic
    username: clinic_user
    password: ${DB_PASSWORD:your_secure_password}
    driver-class-name: org.postgresql.Driver

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # Let Flyway handle schema
    properties:
      hibernate:
        default_schema: public
        jdbc:
          time_zone: UTC

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    schemas: public
```

---

## Database Schema Overview

### Core Tables (23 tables)

**Multi-Tenancy & Security (4 tables)**
- `tenants` - Tenant organizations
- `users` - System users (clinic staff, doctors, admin)
- `roles` - Role definitions (ADMIN, DOCTOR, NURSE, etc.)
- `permissions` - Granular permissions

**Patient Care (7 tables)**
- `patients` - Patient master data
- `appointments` - Patient appointments
- `medical_records` - Clinical records
- `vitals` - Vital signs measurements
- `diagnoses` - ICD-10 coded diagnoses
- `prescriptions` - Medication prescriptions
- `patient_documents` - Patient documents (scans, reports)

**Laboratory (2 tables)**
- `lab_tests` - Lab test orders
- `lab_results` - Lab test results

**Operations (6 tables)**
- `inventory` - Medical supplies inventory
- `inventory_transactions` - Stock movements
- `billing` - Invoice and payment records
- `notifications` - System notifications
- `staff_schedules` - Staff availability
- `consent_records` - DPDP Act 2023 compliance

**Audit (1 table)**
- `audit_logs` - Partitioned audit trail

**Materialized Views (3 views)** - Expensive Aggregations
- `mv_patient_clinical_summary` - Patient dashboard (15min refresh)
- `mv_billing_summary_by_period` - Financial reports (1hr refresh)
- `mv_user_notification_summary` - Notification badges (5min refresh)

**Read Views (26 views)** - CQRS Pattern
- Patient Domain (5): v_patient_list, v_patient_detail, v_patient_appointments, etc.
- Clinical Domain (7): v_appointment_list, v_prescription_list, v_lab_test_list, etc.
- Operations Domain (7): v_billing_list, v_inventory_list, v_notification_list, etc.
- Security Domain (4): v_user_list, v_user_detail, v_role_permissions, v_tenant_summary
- Dashboard (3): v_today_appointments, v_pending_lab_tests, v_overdue_payments

---

## Database Features

### 1. Multi-Tenancy with Row-Level Security (RLS)

All tables include `tenant_id` with RLS policies enforcing data isolation.

**Example Policy:**
```sql
CREATE POLICY tenant_isolation_policy ON patients
    USING (tenant_id = current_setting('app.current_tenant_id')::UUID);
```

**Setting Tenant Context (Application):**
```java
@Before
public void setTenantContext() {
    UUID tenantId = getCurrentTenantId();
    entityManager.createNativeQuery(
        "SET LOCAL app.current_tenant_id = :tenantId"
    ).setParameter("tenantId", tenantId.toString())
     .executeUpdate();
}
```

### 2. Soft Delete Pattern

Most tables support soft delete via `deleted_at` timestamp.

**Implementation:**
```sql
ALTER TABLE patients ADD COLUMN deleted_at TIMESTAMP;

-- Query only active records
SELECT * FROM patients WHERE deleted_at IS NULL AND tenant_id = :tenantId;
```

**Exceptions (Hard Delete Only):**
- `notifications` - No business need for soft delete
- `consent_records` - Compliance requirement for permanent records
- `audit_logs` - Immutable audit trail

### 3. JSONB for Flexible Metadata

Selected tables use JSONB for extensible metadata:

```sql
-- Example: Patient metadata
{
  "emergency_contact": {
    "name": "John Doe",
    "relationship": "Spouse",
    "phone": "+91-9876543210"
  },
  "insurance": {
    "provider": "XYZ Insurance",
    "policy_number": "POL123456"
  },
  "preferences": {
    "language": "en",
    "communication_method": "email"
  }
}
```

**Query JSONB:**
```sql
-- Find patients with specific insurance provider
SELECT * FROM patients
WHERE metadata @> '{"insurance": {"provider": "XYZ Insurance"}}'::jsonb;

-- Extract specific field
SELECT metadata->>'emergency_contact'->>'name' AS emergency_contact_name
FROM patients;
```

### 4. Custom Enum Types

Type-safe enumerations for domain values:

```sql
-- Appointment status
CREATE TYPE appointment_status_enum AS ENUM (
    'SCHEDULED', 'CONFIRMED', 'IN_PROGRESS',
    'COMPLETED', 'CANCELLED', 'NO_SHOW'
);

-- Payment status
CREATE TYPE payment_status_enum AS ENUM (
    'PENDING', 'PARTIALLY_PAID', 'PAID',
    'OVERDUE', 'REFUNDED', 'WRITTEN_OFF'
);

-- User status
CREATE TYPE user_status_enum AS ENUM (
    'ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION'
);
```

**List all enums:**
```sql
SELECT typname FROM pg_type WHERE typcategory = 'E' ORDER BY typname;
```

### 5. Partitioned Audit Logs

Audit logs partitioned by month for performance:

```sql
-- Parent table
CREATE TABLE audit_logs (...) PARTITION BY RANGE (created_at);

-- Monthly partitions
CREATE TABLE audit_logs_2026_01 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

CREATE TABLE audit_logs_2026_02 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
```

**Automatic Partition Management:**
```sql
-- Future: Create partitions automatically
-- Can use pg_partman extension or application-level partition creation
```

### 6. Materialized Views

Pre-computed views for high-performance queries:

- `mv_patient_clinical_summary` - Patient dashboard (15min refresh)
- `mv_billing_summary_by_period` - Financial reports (1hr refresh)
- `mv_user_notification_summary` - Notification badges (5min refresh)

See: `docs/database/materialized-views/` for details.

---

## Database Access

### Via Docker (Recommended)

```bash
# PostgreSQL CLI
docker exec -it clinic-postgres psql -U clinic_user -d clinic

# Execute SQL file
docker exec -i clinic-postgres psql -U clinic_user -d clinic < script.sql

# Run single command
docker exec clinic-postgres psql -U clinic_user -d clinic -c "SELECT COUNT(*) FROM patients;"
```

### Via psql Client (If Installed)

```bash
psql -h localhost -p 5432 -U clinic_user -d clinic
```

### Via Application

```bash
cd clinic-backend
./gradlew bootRun

# Application connects automatically using application.yml configuration
```

---

## Useful Database Commands

### Schema Information

```sql
-- List all tables
\dt

-- Describe table structure
\d+ patients

-- List all enums
\dT

-- List all materialized views
\dm

-- List all indexes
\di

-- List all functions
\df

-- List all users/roles
\du

-- Show table sizes
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### RLS Policies

```sql
-- View all RLS policies
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual
FROM pg_policies
ORDER BY tablename;

-- Check if RLS is enabled on a table
SELECT relname, relrowsecurity FROM pg_class WHERE relname = 'patients';
```

### Partition Information

```sql
-- List all partitions
SELECT
    inhrelid::regclass AS partition,
    pg_get_expr(relpartbound, inhrelid) AS bounds
FROM pg_inherits
WHERE inhparent = 'audit_logs'::regclass;
```

### View Materialized View Status

```sql
-- Check last refresh time
SELECT matviewname, last_refresh, ispopulated
FROM pg_matviews
WHERE matviewname LIKE 'mv_%';

-- Refresh a view manually
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_patient_clinical_summary;
```

---

## Database Backups

### Backup

```bash
# Full database backup
docker exec clinic-postgres pg_dump -U clinic_user -d clinic > clinic_backup_$(date +%Y%m%d).sql

# Schema only
docker exec clinic-postgres pg_dump -U clinic_user -d clinic --schema-only > clinic_schema.sql

# Data only
docker exec clinic-postgres pg_dump -U clinic_user -d clinic --data-only > clinic_data.sql

# Specific table
docker exec clinic-postgres pg_dump -U clinic_user -d clinic -t patients > patients_backup.sql
```

### Restore

```bash
# Restore full database
docker exec -i clinic-postgres psql -U clinic_user -d clinic < clinic_backup.sql

# Create new database and restore
docker exec clinic-postgres psql -U clinic_user -c "CREATE DATABASE clinic_test;"
docker exec -i clinic-postgres psql -U clinic_user -d clinic_test < clinic_backup.sql
```

---

## Troubleshooting

### Connection Issues

**Problem:** Cannot connect to PostgreSQL

**Solutions:**
```bash
# Check container is running
docker ps | grep clinic-postgres

# Check container logs
docker logs clinic-postgres

# Restart container
docker restart clinic-postgres

# Test connection
docker exec clinic-postgres psql -U clinic_user -d clinic -c "SELECT 1;"
```

### Migration Failures

**Problem:** Flyway migration failed

**Solutions:**
```bash
# Check migration history
docker exec clinic-postgres psql -U clinic_user -d clinic -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"

# Repair failed migration
./gradlew flywayRepair

# Baseline (for existing database)
./gradlew flywayBaseline

# Re-run migrations
./gradlew flywayMigrate
```

### Performance Issues

**Problem:** Slow queries

**Diagnostics:**
```sql
-- Find slow queries
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;

-- Check table bloat
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
    n_live_tup AS live_tuples,
    n_dead_tup AS dead_tuples
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;

-- Vacuum analyze
VACUUM ANALYZE;
```

### Disk Space

**Problem:** Running out of disk space

**Check usage:**
```sql
-- Database size
SELECT pg_size_pretty(pg_database_size('clinic'));

-- Largest tables
SELECT
    tablename,
    pg_size_pretty(pg_total_relation_size('public.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size('public.'||tablename) DESC
LIMIT 10;
```

---

## Security Best Practices

### 1. Use Strong Passwords

```bash
# Generate secure password
openssl rand -base64 32

# Update password
docker exec clinic-postgres psql -U clinic_user -d clinic -c "ALTER USER clinic_user WITH PASSWORD 'new_secure_password';"
```

### 2. Restrict Network Access

```yaml
# docker-compose.yml
services:
  postgres:
    ports:
      - "127.0.0.1:5432:5432"  # Only localhost, not 0.0.0.0:5432
```

### 3. Enable SSL/TLS (Production)

```conf
# postgresql.conf
ssl = on
ssl_cert_file = '/path/to/server.crt'
ssl_key_file = '/path/to/server.key'
```

### 4. Regular Backups

```bash
# Daily backup cron job
0 2 * * * docker exec clinic-postgres pg_dump -U clinic_user -d clinic | gzip > /backup/clinic_$(date +\%Y\%m\%d).sql.gz
```

### 5. Monitor Access Logs

```sql
-- Enable logging in postgresql.conf
log_connections = on
log_disconnections = on
log_statement = 'all'

-- View logs
docker logs clinic-postgres
```

---

## References

- [PostgreSQL 16 Documentation](https://www.postgresql.org/docs/16/)
- [Row-Level Security](https://www.postgresql.org/docs/16/ddl-rowsecurity.html)
- [Partitioning](https://www.postgresql.org/docs/16/ddl-partitioning.html)
- [JSONB Functions](https://www.postgresql.org/docs/16/functions-json.html)
- [Materialized Views](https://www.postgresql.org/docs/16/rules-materializedviews.html)
- [Flyway Documentation](https://flywaydb.org/documentation/)

---

## Next Steps

1. Review migration files: `clinic-migrations/src/main/resources/db/migration/`
2. Understand materialized views: `docs/database/materialized-views/`
3. Explore migration guide: `docs/database/migrations.md`
4. Check architecture docs: `docs/architecture/`
