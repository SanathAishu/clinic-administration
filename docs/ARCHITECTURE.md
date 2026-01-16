# System Architecture

Comprehensive architectural documentation for the Clinic Administration multi-tenant SaaS platform.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Multi-Tenancy Architecture](#multi-tenancy-architecture)
3. [CQRS Pattern Implementation](#cqrs-pattern-implementation)
4. [Technology Stack](#technology-stack)
5. [Module Structure](#module-structure)
6. [Data Flow](#data-flow)
7. [Security Architecture](#security-architecture)
8. [Caching Strategy](#caching-strategy)
9. [Design Patterns](#design-patterns)
10. [Deployment Architecture](#deployment-architecture)

## Architecture Overview

The system follows a **multi-tenant, microservices-ready architecture** with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Layer                              │
│         (React Frontend - Vite + TypeScript + TailwindCSS)       │
└────────────────────────┬────────────────────────────────────────┘
                         │ HTTPS / REST API
┌────────────────────────▼────────────────────────────────────────┐
│                   Security Layer (Spring Security 6)             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ JWT Authentication | CORS | Method Security (@PreAuthorize)│  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────┐
│              Application Layer (Spring Boot 3.3.7)               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Controllers  │─▶│   Services   │─▶│ Repositories │          │
│  │  (REST API)  │  │ (Business    │  │ (Data Access)│          │
│  │              │  │  Logic)      │  │              │          │
│  └──────────────┘  └──────────────┘  └──────┬───────┘          │
│                                              │                   │
│  ┌──────────────────────────────────────────┼────────────────┐  │
│  │         Tenant Context (ThreadLocal)     │                │  │
│  │     Multi-tenant Row Level Security      │                │  │
│  └──────────────────────────────────────────┼────────────────┘  │
└─────────────────────────────────────────────┼───────────────────┘
                                              │
                    ┌─────────────────────────┴─────────────────────┐
                    │                                               │
┌───────────────────▼──────────────┐   ┌──────────────────────────▼─┐
│   PostgreSQL 16 (Primary DB)     │   │  Redis 7 (Distributed Cache)│
│  ┌────────────────────────────┐  │   │  - Session data             │
│  │ Core Tables (23 tables)    │  │   │  - User cache               │
│  │ - Multi-tenant with RLS    │  │   │  - Patient cache            │
│  │ - Row Level Security       │  │   │  - Appointment cache        │
│  │ - Soft Delete Only         │  │   │  - Billing cache            │
│  └────────────────────────────┘  │   │  - 7 cache regions          │
│                                   │   │  - TTL-based eviction       │
│  ┌────────────────────────────┐  │   └─────────────────────────────┘
│  │ Materialized Views (3)     │  │
│  │ - Patient clinical summary │  │   ┌─────────────────────────────┐
│  │ - Billing summary          │  │   │    MinIO (Object Storage)   │
│  │ - Notification summary     │  │   │  - Patient documents        │
│  │ - Auto-refresh (scheduled) │  │   │  - Medical images           │
│  └────────────────────────────┘  │   │  - S3-compatible API        │
│                                   │   └─────────────────────────────┘
│  ┌────────────────────────────┐  │
│  │ CQRS Read Views (26 views) │  │   ┌─────────────────────────────┐
│  │ - Patient domain (5 views) │  │   │   RabbitMQ (Message Queue)  │
│  │ - Clinical domain (7 views)│  │   │  - Async notifications      │
│  │ - Operations (7 views)     │  │   │  - Audit events             │
│  │ - Security (4 views)       │  │   │  - Email queue              │
│  │ - Dashboard (3 views)      │  │   └─────────────────────────────┘
│  └────────────────────────────┘  │
│                                   │   ┌─────────────────────────────┐
│  ┌────────────────────────────┐  │   │  Monitoring Stack           │
│  │ Audit Logs (Partitioned)   │  │   │  - Prometheus (metrics)     │
│  │ - Monthly partitions       │  │   │  - Grafana (dashboards)     │
│  │ - 7-year retention         │  │   │  - ELK Stack (logs)         │
│  └────────────────────────────┘  │   │  - Spring Boot Actuator     │
└───────────────────────────────────┘   └─────────────────────────────┘
```

### Key Architectural Principles

1. **Multi-Tenancy First**: Row Level Security ensures complete data isolation
2. **CQRS Pattern**: Separate read (views) and write (entities) models
3. **Caching Strategy**: Redis distributed cache with tenant-aware keys
4. **Soft Delete Only**: Regulatory compliance (7-year retention)
5. **Event-Driven**: RabbitMQ for async operations and notifications
6. **Observability**: Comprehensive monitoring and logging

## Multi-Tenancy Architecture

### Implementation Strategy: Row Level Security (RLS)

The system uses **PostgreSQL Row Level Security** for complete tenant isolation at the database level.

#### How It Works

```java
// 1. Client sends request with JWT containing tenant ID
Authorization: Bearer <jwt-token>

// 2. JwtAuthenticationFilter extracts tenant ID from JWT
UUID tenantId = jwtTokenProvider.getTenantId(token);

// 3. TenantContext stores tenant ID in ThreadLocal
TenantContext.setCurrentTenant(tenantId);

// 4. Hibernate interceptor sets PostgreSQL session variable
SET app.tenant_id = '550e8400-e29b-41d4-a716-446655440000';

// 5. RLS policies automatically filter all queries
CREATE POLICY tenant_isolation ON patients
    USING (tenant_id::text = current_setting('app.tenant_id', TRUE));

// 6. All queries automatically filtered by tenant
SELECT * FROM patients;  -- Only returns current tenant's patients
```

#### Tenant Context Flow

```
Request → JWT Filter → TenantContext.set() → Hibernate Interceptor
    → PostgreSQL Session Variable → RLS Policies → Filtered Results
```

### Benefits of RLS-based Multi-Tenancy

| Aspect | Benefit |
|--------|---------|
| **Security** | Defense-in-depth: DB-level isolation prevents application bugs from leaking data |
| **Performance** | No application-layer filtering, database optimizes queries |
| **Simplicity** | Application code doesn't need tenant filtering logic |
| **Auditability** | All tenant access logged at database level |
| **Scalability** | Single database for all tenants (shared schema) |

### Tenant-Aware Entities

```java
@MappedSuperclass
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @PrePersist
    void setTenantId() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getCurrentTenant();
        }
    }
}
```

## CQRS Pattern Implementation

### Command Query Responsibility Segregation

The system separates **write operations** (commands) from **read operations** (queries).

#### Write Path: JPA Entities

```java
// Complex entity relationships for data integrity
@Entity
@Table(name = "prescriptions")
public class Prescription extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id")
    private MedicalRecord medicalRecord;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL)
    private Set<PrescriptionItem> items;

    // Business logic, validation, state management
}
```

#### Read Path: Database Views

```sql
-- Optimized view with pre-joined data
CREATE OR REPLACE VIEW v_prescription_list AS
SELECT
    p.id,
    p.tenant_id,
    p.prescription_number,
    pat.first_name || ' ' || pat.last_name AS patient_name,
    u.first_name || ' ' || u.last_name AS doctor_name,
    p.prescribed_date,
    p.status,
    COUNT(pi.id) AS medication_count,
    ARRAY_AGG(pi.medication_name) AS medications
FROM prescriptions p
JOIN patients pat ON p.patient_id = pat.id
JOIN users u ON p.prescribed_by = u.id
LEFT JOIN prescription_items pi ON p.id = pi.prescription_id
WHERE p.deleted_at IS NULL
GROUP BY p.id, pat.id, u.id;
```

#### Java View DTOs

```java
// Simple DTO projected from database view
@Immutable
@Entity
@Table(name = "v_prescription_list")
public class PrescriptionListViewDTO {

    @Id
    private UUID id;
    private UUID tenantId;
    private String prescriptionNumber;
    private String patientName;
    private String doctorName;
    private LocalDate prescribedDate;
    private String status;
    private Integer medicationCount;
    private String[] medications;

    // Getters only (immutable)
}
```

### CQRS Benefits

| Command (Write) | Query (Read) |
|-----------------|--------------|
| Complex entity relationships | Simple flat DTOs |
| JPA cascade operations | Pre-joined database views |
| Business logic enforcement | Database-optimized queries |
| Transaction management | Read-only, no side effects |
| Validation and constraints | Fast, cached responses |

### View Inventory

#### Patient Domain (5 views)
- `v_patient_list` - Patient listing with age calculation
- `v_patient_detail` - Full patient profile with latest vitals
- `v_patient_appointments` - Patient appointment history
- `v_patient_medical_records` - Patient clinical records
- `v_patient_billing` - Patient billing summary

#### Clinical Domain (7 views)
- `v_appointment_list` - Appointment schedule
- `v_appointment_detail` - Full appointment details
- `v_prescription_list` - Prescription listing
- `v_prescription_detail` - Prescription with medications
- `v_lab_test_list` - Lab test orders
- `v_lab_test_detail` - Lab results
- `v_medical_record_list` - Clinical encounters

#### Operations Domain (7 views)
- `v_billing_list` - Billing records
- `v_billing_detail` - Invoice details
- `v_inventory_list` - Stock listing
- `v_notification_list` - User notifications
- `v_staff_schedule` - Staff availability
- `v_audit_log_list` - Audit trail
- `v_consent_records` - DPDP compliance

#### Security Domain (4 views)
- `v_user_list` - User directory
- `v_user_detail` - User profile with roles
- `v_role_permissions` - Role permission matrix
- `v_tenant_summary` - Tenant statistics

#### Dashboard Views (3 views)
- `v_today_appointments` - Today's schedule
- `v_pending_lab_tests` - Pending lab work
- `v_overdue_payments` - Outstanding bills

## Technology Stack

### Backend

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Runtime | Java (Eclipse Temurin) | 21 LTS | Application runtime |
| Framework | Spring Boot | 3.3.7 | Web framework |
| Security | Spring Security | 6.x | Authentication & authorization |
| Data Access | Spring Data JPA | 3.3.x | ORM abstraction |
| ORM | Hibernate | 6.4+ | Object-relational mapping |
| Migrations | Flyway | 10.x | Database version control |
| DTO Mapping | MapStruct | 1.5+ | Compile-time mapping |
| Validation | Hibernate Validator | 8.x | Bean validation |
| API Docs | SpringDoc OpenAPI | 2.x | Swagger/OpenAPI |
| Utilities | Lombok | 1.18.x | Boilerplate reduction |

### Infrastructure

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Database | PostgreSQL | 16 | Primary data store |
| Cache | Redis | 7 | Distributed caching |
| Object Storage | MinIO | Latest | Document storage (S3-compatible) |
| Message Queue | RabbitMQ | 3 | Async messaging |
| Metrics | Prometheus | Latest | Time-series metrics |
| Dashboards | Grafana | Latest | Metrics visualization |
| Log Storage | Elasticsearch | 8.12.0 | Centralized logging |
| Log Processing | Logstash | 8.12.0 | Log aggregation |
| Log Visualization | Kibana | 8.12.0 | Log exploration |
| Containerization | Docker | Latest | Container runtime |
| Orchestration | Docker Compose | Latest | Multi-container apps |

### Frontend (Future)

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Framework | React | 18.x | UI framework |
| Language | TypeScript | 5.x | Type-safe JavaScript |
| Build Tool | Vite | 5.x | Fast build tool |
| Styling | Tailwind CSS | 3.x | Utility-first CSS |
| UI Components | shadcn/ui + Radix | Latest | Accessible components |
| State (Server) | TanStack Query | 5.x | Server state management |
| State (Client) | Zustand | 4.x | Client state management |
| Forms | React Hook Form | 7.x | Form management |
| Validation | Zod | 3.x | Schema validation |

## Module Structure

The project follows a **multi-module Gradle structure**:

```
clinic-administration/
├── clinic-backend/              # Main Spring Boot application
│   ├── controller/              # REST API endpoints
│   ├── service/                 # Business logic layer
│   ├── repository/              # Data access layer
│   ├── mapper/                  # MapStruct DTO mappers
│   ├── config/                  # Spring configuration
│   └── security/                # JWT authentication
│
├── clinic-common/               # Shared library module
│   ├── entity/                  # JPA entities (25 entities)
│   │   ├── core/                # Tenant, User, Role, Permission
│   │   ├── patient/             # Patient, Vital, Diagnosis
│   │   ├── clinical/            # Appointment, MedicalRecord, Prescription
│   │   └── operational/         # Billing, Inventory, Notification
│   ├── dto/                     # Request/Response DTOs
│   │   ├── auth/                # Login, Refresh, Register DTOs
│   │   ├── request/             # Create/Update request DTOs
│   │   ├── response/            # Response DTOs
│   │   └── view/                # Read view DTOs (CQRS)
│   ├── enums/                   # Enumeration types
│   └── security/                # TenantContext
│
├── clinic-migrations/           # Flyway database migrations
│   └── db/migration/
│       ├── V1__create_foundation_tables.sql
│       ├── V2__create_identity_access_tables.sql
│       ├── V3__create_patient_care_tables.sql
│       ├── V4__create_operations_tables.sql
│       ├── V5__create_materialized_views_phase1.sql
│       └── V6__create_read_views.sql
│
├── docker-compose.yml           # Local development environment
├── monitoring/                  # Prometheus & Grafana config
└── docs/                        # Project documentation
```

### Dependency Flow

```
clinic-backend
    ↓ (depends on)
clinic-common
    ↓ (depends on)
clinic-migrations (independent)
```

## Data Flow

### Write Operation (Command)

```
1. Client Request
   POST /api/patients
   { "firstName": "John", "lastName": "Doe", ... }
   ↓
2. Security Filter (JWT)
   - Validate JWT token
   - Extract tenant ID
   - Set TenantContext
   ↓
3. Controller
   @PostMapping("/api/patients")
   public PatientResponseDTO createPatient(@Valid CreatePatientRequest request)
   ↓
4. Service Layer
   - Business validation
   - Check duplicates
   - Set tenant ID from context
   ↓
5. Repository (JPA)
   - Save entity with relationships
   - Trigger @PrePersist hooks
   - Set created_by, created_at
   ↓
6. Hibernate Interceptor
   - Set PostgreSQL session variable
   - Execute: SET app.tenant_id = '<uuid>'
   ↓
7. PostgreSQL
   - RLS policies filter INSERT
   - Enforce constraints
   - Commit transaction
   ↓
8. Cache Eviction
   @CacheEvict(value = "patients", allEntries = true)
   - Clear Redis cache for patients
   ↓
9. Response
   PatientResponseDTO (mapped via MapStruct)
```

### Read Operation (Query)

```
1. Client Request
   GET /api/patients/list
   ↓
2. Security Filter (JWT)
   - Validate JWT token
   - Extract tenant ID
   - Set TenantContext
   ↓
3. Controller
   @GetMapping("/api/patients/list")
   public List<PatientListViewDTO> getPatientList()
   ↓
4. Service Layer
   @Cacheable(value = "patients", key = "#tenantId + ':list'")
   ↓
5. Cache Check (Redis)
   - Check if key exists: "550e8400:patients:list"
   - If HIT: Return cached data (skip DB)
   - If MISS: Continue to database
   ↓
6. Repository (Read-Only)
   - Query database view: v_patient_list
   - Projection: SELECT id, full_name, age, status...
   ↓
7. PostgreSQL View
   - View already pre-joined
   - Database optimizes query
   - RLS policies filter results
   ↓
8. Cache Store (Redis)
   - Store result in Redis
   - TTL: 5 minutes (patients cache)
   - Key: "<tenantId>:patients:list"
   ↓
9. Response
   List<PatientListViewDTO> (no mapping needed, already DTOs)
```

## Security Architecture

See [SECURITY.md](SECURITY.md) for comprehensive security documentation.

### Key Security Features

1. **JWT-based Authentication**
   - Stateless authentication
   - Token contains tenant ID, user ID, roles
   - Refresh token mechanism (7-day expiration)

2. **Multi-Tenant Isolation**
   - Row Level Security at database level
   - Tenant context in ThreadLocal
   - Cache keys include tenant ID

3. **Method-Level Authorization**
   - `@PreAuthorize("hasRole('DOCTOR')")`
   - `@PreAuthorize("hasPermission(#patientId, 'PATIENT', 'READ')")`

4. **Audit Logging**
   - All data modifications logged
   - Partitioned by month for performance
   - 7-year retention (regulatory compliance)

5. **Input Validation**
   - Bean Validation annotations
   - Custom validators for business rules
   - SQL injection prevention (parameterized queries)

## Caching Strategy

See [CACHING.md](CACHING.md) for detailed caching documentation.

### Cache Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Redis Cache Layer                         │
│  ┌───────────────┐  ┌───────────────┐  ┌────────────────┐  │
│  │ roles         │  │ users         │  │ patients       │  │
│  │ TTL: 10 min   │  │ TTL: 10 min   │  │ TTL: 5 min     │  │
│  └───────────────┘  └───────────────┘  └────────────────┘  │
│  ┌───────────────┐  ┌───────────────┐  ┌────────────────┐  │
│  │ appointments  │  │ appointments: │  │ billings       │  │
│  │ TTL: 2 min    │  │ today         │  │ TTL: 3 min     │  │
│  │               │  │ TTL: 1 min    │  │                │  │
│  └───────────────┘  └───────────────┘  └────────────────┘  │
│  ┌───────────────┐                                          │
│  │ billings:     │                                          │
│  │ summary       │   Key Format:                            │
│  │ TTL: 10 min   │   {tenantId}:{cacheName}:{params}       │
│  └───────────────┘                                          │
└─────────────────────────────────────────────────────────────┘
```

### Cache Regions

| Cache Name | TTL | Volatility | Use Case |
|------------|-----|------------|----------|
| `roles` | 10 min | Low | User roles & permissions |
| `users` | 10 min | Low | User entities, profiles |
| `patients` | 5 min | Medium | Patient demographics |
| `appointments` | 2 min | High | Appointment schedules |
| `appointments:today` | 1 min | Very High | Today's appointments |
| `billings` | 3 min | Medium | Billing records |
| `billings:summary` | 10 min | Low | Historical summaries |

### Cache Key Generation

```java
@Component
public class TenantAwareCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String methodName = method.getName();
        String paramsKey = Arrays.stream(params)
            .map(Object::toString)
            .collect(Collectors.joining(":"));

        return String.format("%s:%s:%s", tenantId, methodName, paramsKey);
    }
}
```

### Cache Invalidation Patterns

1. **Write-Through**: Update cache on every write
2. **Write-Behind**: Async cache update
3. **Cache-Aside**: Application manages cache
4. **TTL-Based**: Automatic expiration

```java
// Cache eviction on write
@CacheEvict(value = "users", allEntries = true)
public User updateUser(UUID id, UUID tenantId, User updates) {
    // Update logic
}

// Conditional caching
@Cacheable(value = "users", key = "#tenantId + ':list'",
           unless = "#result.isEmpty()")
public List<UserListViewDTO> getUserList(UUID tenantId) {
    // Query logic
}
```

## Design Patterns

### 1. Repository Pattern

```java
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Patient> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);
}
```

### 2. Service Layer Pattern

```java
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Transactional
    @CacheEvict(value = "patients", allEntries = true)
    public PatientResponseDTO createPatient(CreatePatientRequest request) {
        // Business logic
    }
}
```

### 3. DTO Pattern (MapStruct)

```java
@Mapper(componentModel = "spring")
public interface PatientMapper {

    PatientResponseDTO toResponseDTO(Patient patient);

    Patient toEntity(CreatePatientRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(@MappingTarget Patient patient, UpdatePatientRequest request);
}
```

### 4. Builder Pattern (Lombok)

```java
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient extends TenantAwareEntity {
    // Fields
}

// Usage
Patient patient = Patient.builder()
    .firstName("John")
    .lastName("Doe")
    .build();
```

### 5. Strategy Pattern (State Machines)

```java
public interface AppointmentStateMachine {
    void validateTransition(AppointmentStatus from, AppointmentStatus to);
}

@Service
public class AppointmentStateMachineImpl implements AppointmentStateMachine {

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> TRANSITIONS = Map.of(
        AppointmentStatus.SCHEDULED, Set.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED),
        AppointmentStatus.CONFIRMED, Set.of(AppointmentStatus.CHECKED_IN, AppointmentStatus.CANCELLED)
    );

    @Override
    public void validateTransition(AppointmentStatus from, AppointmentStatus to) {
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidStateTransitionException(from, to);
        }
    }
}
```

## Deployment Architecture

### Docker Compose Stack

```yaml
services:
  postgres:      # PostgreSQL 16 (primary database)
  redis:         # Redis 7 (distributed cache)
  minio:         # MinIO (S3-compatible object storage)
  rabbitmq:      # RabbitMQ 3 (message broker)
  prometheus:    # Prometheus (metrics)
  grafana:       # Grafana (dashboards)
  elasticsearch: # Elasticsearch (log storage)
  logstash:      # Logstash (log processing)
  kibana:        # Kibana (log visualization)
```

### Container Network

```
┌─────────────────────────────────────────────────────────┐
│                   clinic-network (bridge)                │
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ Backend  │─▶│ Postgres │  │  Redis   │             │
│  │  :8080   │  │  :5432   │  │  :6379   │             │
│  └────┬─────┘  └──────────┘  └──────────┘             │
│       │                                                 │
│       ├──────────▶┌──────────┐  ┌──────────┐           │
│       │           │  MinIO   │  │ RabbitMQ │           │
│       │           │  :9000   │  │  :5672   │           │
│       │           └──────────┘  └──────────┘           │
│       │                                                 │
│       └──────────▶┌──────────┐  ┌──────────┐           │
│                   │Prometheus│  │ Elastic  │           │
│                   │  :9090   │  │  :9200   │           │
│                   └──────────┘  └──────────┘           │
└─────────────────────────────────────────────────────────┘
```

### Resource Allocation

| Service | CPU | Memory | Storage |
|---------|-----|--------|---------|
| PostgreSQL | 2 cores | 2-4 GB | 50-100 GB |
| Redis | 0.5 cores | 512 MB - 1 GB | 2 GB |
| MinIO | 0.5-1 cores | 1-2 GB | 50-100 GB |
| RabbitMQ | 0.5 cores | 512 MB - 1 GB | 5 GB |
| Spring Boot | 2-3 cores | 2-4 GB | 1 GB |
| Prometheus | 0.5 cores | 1-2 GB | 10 GB |
| Elasticsearch | 1-2 cores | 2-4 GB | 10-20 GB |

### Production Deployment Recommendations

**Recommended VM: GCP n2-standard-8**
- 8 vCPUs
- 32 GB RAM
- 500 GB SSD
- Cost: ₹11,120/month (1-year CUD)

See [VM_SPECS_AND_COSTING.md](docs/VM_SPECS_AND_COSTING.md) for detailed deployment specifications.

## Scalability Considerations

### Horizontal Scaling

1. **Stateless Application**: No server-side session state (JWT-based auth)
2. **Distributed Cache**: Redis can be clustered
3. **Database Connection Pooling**: HikariCP with optimal settings
4. **Read Replicas**: PostgreSQL read replicas for query scaling

### Vertical Scaling

1. **Database**: Increase PostgreSQL resources as data grows
2. **Cache**: Increase Redis memory for larger datasets
3. **Application**: Increase JVM heap size for concurrent requests

### Performance Optimization

1. **Database Indexes**: Comprehensive indexing strategy
2. **Query Optimization**: Database views pre-join data
3. **Caching**: Multi-tier caching (Redis + materialized views)
4. **Connection Pooling**: HikariCP optimized configuration
5. **Lazy Loading**: JPA entities use lazy fetching
6. **Pagination**: All list endpoints support pagination

## Monitoring and Observability

### Metrics (Prometheus + Grafana)

- JVM metrics (heap, GC, threads)
- HTTP request metrics (rate, duration, errors)
- Database connection pool metrics
- Cache hit/miss rates
- Custom business metrics

### Logging (ELK Stack)

- Application logs (Spring Boot)
- Access logs (Nginx)
- Database slow query logs
- Security audit logs

### Tracing (Future: OpenTelemetry)

- Distributed tracing across services
- Request flow visualization
- Performance bottleneck identification

## Disaster Recovery

### Backup Strategy

1. **Database**: Daily automated backups (pg_dump)
2. **Documents**: MinIO object storage replication
3. **Configuration**: Version-controlled in Git
4. **Retention**: 30-day retention for backups

### Recovery Procedures

1. **Database Restore**: Point-in-time recovery (PITR)
2. **Document Restore**: MinIO bucket versioning
3. **Configuration Restore**: Git checkout + redeploy

## Compliance and Security

### Regulatory Compliance

1. **DPDP Act 2023**: Consent management, data breach notification
2. **IT Act 2000**: Encryption, access controls, audit logs
3. **ABDM Guidelines**: ABHA ID integration
4. **Clinical Establishments Act**: 7-year record retention

### Security Measures

1. **Encryption at Rest**: Database TDE, encrypted volumes
2. **Encryption in Transit**: TLS 1.3 for all communications
3. **Access Control**: RBAC with fine-grained permissions
4. **Audit Logging**: Comprehensive audit trail
5. **Data Isolation**: Multi-tenant RLS

## Future Enhancements

1. **Microservices Migration**: Break monolith into services
2. **GraphQL API**: Add GraphQL alongside REST
3. **Real-time Updates**: WebSocket support for live data
4. **Mobile Apps**: Native iOS/Android apps
5. **AI/ML Integration**: Predictive analytics, recommendations
6. **HL7 FHIR**: Healthcare interoperability standard
7. **Kubernetes**: Production orchestration
8. **Service Mesh**: Istio for advanced networking

## References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/3.3.7/reference/htmlsingle/)
- [PostgreSQL 16 Documentation](https://www.postgresql.org/docs/16/)
- [Redis Documentation](https://redis.io/docs/)
- [Docker Documentation](https://docs.docker.com/)
- [PROJECT_SPECIFICATION.md](docs/PROJECT_SPECIFICATION.md)
- [CACHING.md](CACHING.md)
- [SECURITY.md](SECURITY.md)
- [API.md](API.md)

---

**Document Version**: 1.0
**Last Updated**: 2026-01-16
**Maintained By**: Development Team
