# Clinic Administration System

A comprehensive, multi-tenant clinic management system for the Indian healthcare market with DPDP Act 2023 compliance, ABDM integration, and production-ready architecture.

## 📚 Documentation

**Complete documentation is available in the [`docs/`](docs/) directory:**

### Core Documentation
- **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** - System architecture, multi-tenancy, CQRS pattern
- **[SECURITY.md](docs/SECURITY.md)** - Authentication, authorization, compliance
- **[CACHING.md](docs/CACHING.md)** - Redis distributed caching strategy
- **[API.md](docs/API.md)** - REST API documentation and examples
- **[Getting Started Guide](docs/README.md)** - Comprehensive project documentation
- **[CLAUDE.md](docs/CLAUDE.md)** - Claude Code instructions and development guidelines

### Phase Documentation
- **[Phase E: Quality Assurance & Compliance (ISO 27001)](docs/PHASE_E_IMPLEMENTATION.md)** - Compliance Dashboard, Data Retention, Audit Trails ✅
- **Phase D: Operations Research & Optimization** - Queue Management, Inventory Optimization, Prescription Enhancement ✅
- **Phase A-C: Foundation** - Core infrastructure, security, caching ✅

### Database Documentation
- **[Database Setup](docs/database/setup.md)** - PostgreSQL configuration and access
- **[Database Migrations](docs/database/migrations.md)** - Flyway migration guide
- **[Materialized Views](docs/database/materialized-views/)** - Performance optimization (Phase 1 complete ✅)
- **[Read Views (CQRS)](docs/database/read-views.md)** - 26 database views for optimized READ operations ✅

### Project Specifications
- **[Project Specification](docs/PROJECT_SPECIFICATION.md)** - Complete technical specification
- **[VM Specs & Costing](docs/VM_SPECS_AND_COSTING.md)** - Production deployment guide

---

## Features

### Phase E: Quality Assurance & Compliance ✅
- **Compliance Dashboard** - Statistical Process Control (SPC) with 3-Sigma Rule for anomaly detection
- **SLA Monitoring** - 7 metric types with automatic out-of-control detection
- **Automated Data Retention** - Configurable archival policies with 4 actions (soft delete, export, anonymize, hard delete)
- **Sensitive Data Audit Trail** - Append-only immutable logs for all sensitive operations with AOP-based automatic logging
- **ISO 27001 Alignment** - Controls A.12.4 (logging & monitoring), A.18 (compliance)
- **Data Lifecycle Management** - Pareto Principle optimization (80% queries on 20% of data)

### Core Features ✅
- **Multi-tenant SaaS** architecture with PostgreSQL Row Level Security (RLS)
- **JWT Authentication** - Stateless authentication with 15-minute access tokens, 7-day refresh tokens
- **RBAC Authorization** - Fine-grained role-based access control with method-level security
- **Redis Distributed Caching** - 7 cache regions with tenant-aware keys and TTL-based eviction
- **CQRS Pattern** - 26 database read views for optimized queries
- **Materialized Views** - 3 views for expensive aggregations (90-95% performance improvement)
- **Audit Logging** - Comprehensive audit trail with 7-year retention (monthly partitions)
- **Soft Delete Only** - Regulatory compliance with data retention requirements
- **Input Validation** - Bean Validation with custom validators
- **Security** - Multi-layer security with encryption, XSS/CSRF protection, SQL injection prevention

### Phase D: Operations Research ✅
- **Queue Management** - M/M/1 Queuing Theory with ρ utilization tracking and Little's Law validation
- **Inventory Optimization** - Economic Order Quantity (EOQ = √(2DS/H)) and Reorder Point calculation with safety stock
- **Prescription Enhancement** - Drug interaction checking with inventory integration and ACID transactions

### Compliance ✅
- **DPDP Act 2023** - Consent management, data breach notification, right to erasure
- **IT Act 2000** - Encryption, access controls, audit logs
- **ABDM Guidelines** - ABHA ID integration (14-digit unique health identifier)
- **Clinical Establishments Act** - 7-year minimum record retention

### Infrastructure ✅
- **PostgreSQL 16** - Primary database with RLS, partitioning, materialized views
- **Redis 7** - Distributed caching layer
- **MinIO** - S3-compatible object storage for documents
- **RabbitMQ** - Message broker for async operations
- **Prometheus + Grafana** - Metrics collection and visualization
- **ELK Stack** - Centralized logging (Elasticsearch, Logstash, Kibana)
- **Docker Compose** - Containerized local development environment

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
| Validation | Hibernate Validator | 8.x | Bean validation (JSR-380) |
| API Docs | SpringDoc OpenAPI | 2.x | Swagger/OpenAPI 3.0 |
| Utilities | Lombok | 1.18.x | Boilerplate reduction |
| JWT | jjwt (Java JWT) | 0.12.x | JWT token handling |

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

## Project Structure

```
clinic-administration/
├── clinic-backend/              # Spring Boot application
│   ├── annotation/              # Custom annotations (@LogSensitiveAccess)
│   ├── aspect/                  # AOP aspects (SensitiveDataAccessAspect)
│   ├── controller/              # REST API endpoints (12 controllers)
│   ├── service/                 # Business logic (25+ services)
│   ├── repository/              # Data access layer (30+ repositories)
│   ├── mapper/                  # MapStruct DTO mappers (8 mappers)
│   ├── dto/                     # DTOs organized by feature
│   └── security/                # JWT authentication
├── clinic-common/               # Shared library
│   ├── entity/                  # JPA entities (29 entities)
│   │   ├── core/                # Tenant, User, Role, Permission, Session, AuditLog
│   │   ├── patient/             # Patient, Vital, Diagnosis, PatientDocument
│   │   ├── clinical/            # Appointment, MedicalRecord, Prescription, LabTest, etc.
│   │   ├── operational/         # Billing, Inventory, Notification, StaffSchedule, QueueMetrics
│   │   └── compliance/          # ComplianceMetrics, DataRetentionPolicy, SensitiveDataAccessLog (Phase E)
│   ├── enums/                   # Enumerations (ComplianceMetricType, AccessType, etc.)
│   ├── dto/                     # Request/Response DTOs
│   └── security/                # TenantContext, security utilities
├── clinic-migrations/           # Flyway database migrations (19 migrations)
│   └── db/migration/
│       ├── V1-V4                # Core schema (23 tables, Phase A)
│       ├── V5                   # Materialized views (3 views, Phase A)
│       ├── V6                   # Read views - CQRS (26 views, Phase A)
│       ├── V7-V16               # Enhanced features (Phases B-D)
│       ├── V17-V19              # Compliance & QA (Phase E)
├── docs/                        # Project documentation
│   ├── PHASE_E_IMPLEMENTATION.md    # Phase E: Quality Assurance & ISO 27001
│   ├── ARCHITECTURE.md          # System architecture
│   ├── SECURITY.md              # Security & compliance
│   ├── CACHING.md               # Redis caching strategy
│   ├── API.md                   # REST API documentation
│   ├── CLAUDE.md                # Development guidelines
│   ├── PROJECT_SPECIFICATION.md # Technical specification
│   └── [other documentation]
└── docker-compose.yml           # Local development environment
```

## Getting Started

### Prerequisites

- **Java 21 LTS** (Eclipse Temurin recommended)
- **Docker & Docker Compose** (for infrastructure services)
- **Gradle 8.5+** (wrapper included)
- **Git** (for version control)

### Quick Start

1. **Clone the repository:**
```bash
git clone https://github.com/SanathAishu/clinic-administration.git
cd clinic-administration
```

2. **Start infrastructure services:**
```bash
docker compose up -d
```

This will start all required services:
- **PostgreSQL 16** (port 5432) - Primary database
- **Redis 7** (port 6379) - Distributed cache
- **MinIO** (port 9000, console 9001) - Object storage
- **RabbitMQ** (port 5672, management 15672) - Message broker
- **Prometheus** (port 9090) - Metrics collection
- **Grafana** (port 3001) - Metrics dashboards
- **Elasticsearch** (port 9200) - Log storage
- **Logstash** (port 5000) - Log processing
- **Kibana** (port 5601) - Log visualization

3. **Run database migrations:**
```bash
cd clinic-migrations
../gradlew flywayMigrate
```

This will apply 19 migrations across 5 implementation phases:
- **Phase A (V1-V6):** Foundation infrastructure
  - V1: Foundation tables (tenants, users, roles)
  - V2: Identity & access tables (sessions, permissions)
  - V3: Patient care tables (patients, appointments, medical records)
  - V4: Operations tables (billing, inventory, notifications)
  - V5: Materialized views (3 views for expensive aggregations)
  - V6: CQRS read views (26 views for optimized queries)
- **Phase B-C (V7-V14):** Security, caching, performance
- **Phase D (V15-V16):** Operations Research - Queue Management, Inventory Optimization
- **Phase E (V17-V19):** Quality Assurance & ISO 27001 Compliance
  - V17: Compliance metrics (SPC-based monitoring)
  - V18: Data retention policies and archival tracking
  - V19: Sensitive data access audit trail

4. **Build and run the application:**
```bash
cd ../clinic-backend
../gradlew bootRun
```

The application will be available at:
- **Backend API**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

### API Documentation

Once the application is running, access the interactive API documentation:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

See [API.md](docs/API.md) for comprehensive API documentation.

### Monitoring & Observability

- **Grafana**: http://localhost:3001 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Kibana**: http://localhost:5601
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)
- **MinIO Console**: http://localhost:9001 (minioadmin/minioadmin)

## Database Schema

The system uses a comprehensive 23-table schema organized in 4 groups:

### Foundation (5 tables)
- `tenants` - Multi-tenant isolation
- `users` - System users (staff only)
- `roles` - Role definitions
- `user_roles` - User-role assignments
- `audit_logs` - Comprehensive audit trail (partitioned by month)

### Identity & Access (3 tables)
- `sessions` - JWT session tracking
- `permissions` - Fine-grained permissions
- `role_permissions` - Role-permission mappings

### Patient Care (10 tables)
- `patients` - Patient demographics with ABHA
- `appointments` - Appointment scheduling
- `medical_records` - Clinical documentation
- `prescriptions` & `prescription_items` - Prescriptions
- `lab_tests` & `lab_results` - Laboratory tests
- `vitals` - Patient vital signs
- `diagnoses` - ICD-10 coded diagnoses
- `billing` - Billing and invoicing

### Operations (6 tables)
- `inventory` & `inventory_transactions` - Inventory management
- `staff_schedules` - Staff availability
- `notifications` - System notifications
- `consent_records` - DPDP Act compliance
- `patient_documents` - Document metadata

## Development

### Build Commands

```bash
# Clean build
./gradlew clean build

# Build without tests
./gradlew clean build -x test

# Run tests only
./gradlew test

# Run application
./gradlew :clinic-backend:bootRun

# Check dependencies
./gradlew dependencies
```

### Database Operations

```bash
# Run migrations
./gradlew flywayMigrate

# Check migration status
./gradlew flywayInfo

# Validate migrations
./gradlew flywayValidate

# Repair failed migration
./gradlew flywayRepair
```

### Cache Operations

```bash
# Connect to Redis
docker exec -it clinic-redis redis-cli

# List all cache keys
KEYS *

# List keys for specific tenant
KEYS 550e8400-e29b-41d4-a716-446655440000:*

# Get cache value
GET "550e8400-e29b-41d4-a716-446655440000:users:list"

# Clear all cache (use with caution)
FLUSHALL
```

See [CACHING.md](docs/CACHING.md) for comprehensive caching documentation.

## Compliance

- **DPDP Act 2023**: Consent management, breach notification, right to erasure
- **IT Act 2000 + SPDI Rules**: Encryption, access controls, audit logs
- **ABDM Guidelines**: ABHA ID integration
- **Clinical Establishments Act**: 7-year minimum record retention

## Deployment

See [VM_SPECS_AND_COSTING.md](docs/VM_SPECS_AND_COSTING.md) for production deployment specifications and GCP VM recommendations.

For complete technical specifications, see [PROJECT_SPECIFICATION.md](docs/PROJECT_SPECIFICATION.md).

Recommended configuration: n2-standard-8 (8 vCPU, 32GB RAM) - ₹11,120/month with 1-year CUD

## License

Proprietary - All rights reserved

## Support

For issues and questions, please contact the development team.
