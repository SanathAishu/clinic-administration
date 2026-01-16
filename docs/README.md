# Clinic Management System - Documentation

Complete documentation for the Clinic Management System built with Spring Boot 3.3.7, Java 21, and PostgreSQL 16.

---

## 📚 Table of Contents

- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [Database](#database)
- [API](#api)
- [Deployment](#deployment)
- [Contributing](#contributing)

---

## 🚀 Quick Start

### Prerequisites

- **Java 21 (LTS)** - OpenJDK or Oracle JDK
- **PostgreSQL 16** - Running via Docker or installed locally
- **Docker** - For PostgreSQL container (recommended)
- **Gradle 8+** - Build tool (included via wrapper)

### Running the Application

1. **Start PostgreSQL:**
   ```bash
   docker run -d \
     --name clinic-postgres \
     -e POSTGRES_USER=clinic_user \
     -e POSTGRES_PASSWORD=your_password \
     -e POSTGRES_DB=clinic \
     -p 5432:5432 \
     postgres:16
   ```

2. **Build the application:**
   ```bash
   ./gradlew clean build
   ```

3. **Run the application:**
   ```bash
   ./gradlew :clinic-backend:bootRun
   ```

4. **Access the application:**
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - H2 Console (dev): http://localhost:8080/h2-console

### Configuration

**Database connection** (`clinic-backend/src/main/resources/application.yml`):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/clinic
    username: clinic_user
    password: ${DB_PASSWORD:your_password}
```

**Environment variables:**
```bash
export DB_PASSWORD=your_secure_password
export SPRING_PROFILES_ACTIVE=dev
```

---

## 🏗️ Architecture

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Layer                            │
│  (Web UI / Mobile App / External Systems)                   │
└─────────────────────┬───────────────────────────────────────┘
                      │ HTTPS / REST API
┌─────────────────────▼───────────────────────────────────────┐
│                   API Gateway / Load Balancer               │
│                 (Spring Cloud Gateway - Future)             │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│               Application Layer (Spring Boot)               │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐           │
│  │ Controllers│  │  Services  │  │ Repositories│           │
│  │  (REST)    │─▶│ (Business) │─▶│   (Data)    │           │
│  └────────────┘  └────────────┘  └──────┬──────┘           │
│                                          │                   │
│  ┌────────────────────────────────────┐ │                   │
│  │  Security (Spring Security)        │ │                   │
│  │  - JWT Authentication              │ │                   │
│  │  - Role-Based Access Control       │ │                   │
│  │  - Multi-Tenancy (RLS)             │ │                   │
│  └────────────────────────────────────┘ │                   │
└──────────────────────────────────────────┼───────────────────┘
                                           │ JPA/Hibernate
┌──────────────────────────────────────────▼───────────────────┐
│                   Database Layer (PostgreSQL 16)             │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│  │  Core Tables   │  │ Materialized   │  │  Audit Logs    │ │
│  │  (23 tables)   │  │     Views      │  │  (Partitioned) │ │
│  │  - Patients    │  │  - Patient     │  │  - Monthly     │ │
│  │  - Billing     │  │    Summary     │  │    Partitions  │ │
│  │  - Lab Tests   │  │  - Billing     │  │                │ │
│  └────────────────┘  │    Summary     │  └────────────────┘ │
│                      │  - Notification│                      │
│  ┌────────────────┐  │    Summary     │  ┌────────────────┐ │
│  │   Row-Level    │  └────────────────┘  │   Scheduled    │ │
│  │   Security     │                      │   Jobs         │ │
│  │  (Multi-Tenant)│                      │  (App-based)   │ │
│  └────────────────┘                      └────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Module Structure

```
clinic-administration/
├── clinic-backend/              # Main application (Spring Boot)
│   ├── controller/              # REST endpoints (1 controller)
│   ├── service/                 # Business logic (22 services)
│   ├── repository/              # Data access - JPA (23 repositories)
│   ├── mapper/                  # MapStruct DTO mappers (8 mappers)
│   ├── security/                # JWT authentication
│   └── config/                  # Spring configuration
│
├── clinic-common/               # Shared library
│   ├── entity/                  # JPA entities (25 entities)
│   │   ├── core/                # Tenant, User, Role, Permission, Session, AuditLog
│   │   ├── patient/             # Patient, Vital, Diagnosis, PatientDocument
│   │   ├── clinical/            # Appointment, MedicalRecord, Prescription, LabTest
│   │   └── operational/         # Billing, Inventory, Notification, StaffSchedule
│   ├── dto/                     # Request/Response DTOs
│   │   ├── auth/                # Login/Refresh DTOs
│   │   ├── request/             # Create/Update request DTOs
│   │   └── response/            # Response DTOs
│   └── security/                # TenantContext
│
├── clinic-migrations/           # Database migrations (6 migrations)
│   └── db/migration/
│       ├── V1__create_foundation_tables.sql
│       ├── V2__create_identity_access_tables.sql
│       ├── V3__create_patient_care_tables.sql
│       ├── V4__create_operations_tables.sql
│       ├── V5__create_materialized_views_phase1.sql
│       └── V6__create_read_views.sql
│
└── docs/                        # Documentation (this directory)
```

### Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.3.7 |
| Data Access | Spring Data JPA | 3.3.x |
| ORM | Hibernate | 6.4+ |
| Database | PostgreSQL | 16 |
| Migration | Flyway | 10.x |
| Validation | Jakarta Bean Validation | 3.0 |
| Mapping | MapStruct | 1.5+ |
| API Docs | Springdoc OpenAPI | 2.x |
| Testing | JUnit 5, Mockito | 5.x |
| Build | Gradle | 8.x |
| Containerization | Docker | Latest |

---

## 💾 Database

### Database Overview

- **DBMS:** PostgreSQL 16
- **Database Name:** `clinic`
- **Schema:** `public`
- **Tables:** 23 core tables + 3 materialized views + 26 read views
- **Key Features:**
  - Row-Level Security (RLS) for multi-tenancy
  - Materialized views for performance
  - JSONB for flexible metadata
  - Partitioned audit logs
  - Custom enum types (10+ enums)

### Database Documentation

| Document | Description |
|----------|-------------|
| **[Database Setup Guide](database/setup.md)** | PostgreSQL installation, configuration, and access |
| **[Migrations Guide](database/migrations.md)** | Flyway migrations, versioning, and best practices |
| **[Read Views (CQRS)](database/read-views.md)** | 26 database views for optimized READ operations |
| **[Views Design Document](database/views-design.md)** | Comprehensive views design based on repository analysis |
| **[Materialized Views](database/materialized-views/)** | Performance optimization with pre-computed views |
| └─ [README](database/materialized-views/README.md) | Installation and usage guide |
| └─ [Refresh Strategies](database/materialized-views/refresh-strategies.md) | Automated refresh options |
| └─ [Design](database/materialized-views/design.md) | Complete design with all phases |
| └─ [Phase 1 Implementation](database/materialized-views/phase1-implementation.md) | What was built and verification |

### Database Schema (23 Core Tables)

**Multi-Tenancy & Security (4 tables)**
- `tenants` - Tenant organizations
- `users` - System users (staff, doctors, admin)
- `roles` - Role definitions
- `permissions` - Granular permissions

**Patient Care (7 tables)**
- `patients` - Patient master data
- `appointments` - Patient appointments
- `medical_records` - Clinical records
- `vitals` - Vital signs measurements
- `diagnoses` - ICD-10 coded diagnoses
- `prescriptions` - Medication prescriptions
- `patient_documents` - Patient documents

**Laboratory (2 tables)**
- `lab_tests` - Lab test orders
- `lab_results` - Lab test results

**Operations (6 tables)**
- `inventory` - Medical supplies
- `inventory_transactions` - Stock movements
- `billing` - Invoices and payments
- `notifications` - System notifications
- `staff_schedules` - Staff availability
- `consent_records` - DPDP Act 2023 compliance

**Audit (1 table)**
- `audit_logs` - Partitioned audit trail

**Materialized Views (3 views)** - Expensive Aggregations
- `mv_patient_clinical_summary` - Patient dashboard (15min refresh)
- `mv_billing_summary_by_period` - Financial reports (1hr refresh)
- `mv_user_notification_summary` - Notification badges (5min refresh)

**Read Views (26 views)** - CQRS Pattern for Optimized Reads
- Patient Domain (5): v_patient_list, v_patient_detail, v_patient_appointments, etc.
- Clinical Domain (7): v_appointment_list, v_prescription_list, v_lab_test_list, etc.
- Operations Domain (7): v_billing_list, v_inventory_list, v_notification_list, etc.
- Security Domain (4): v_user_list, v_user_detail, v_role_permissions, v_tenant_summary
- Dashboard (3): v_today_appointments, v_pending_lab_tests, v_overdue_payments

### Quick Database Commands

```bash
# Access PostgreSQL CLI
docker exec -it clinic-postgres psql -U clinic_user -d clinic

# List all tables
\dt

# Describe table structure
\d+ patients

# List materialized views
\dm

# Check migration history
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

# Refresh materialized views
SELECT refresh_patient_clinical_summary();
SELECT refresh_billing_summary();
SELECT refresh_notification_summary();
```

---

## 🔌 API

### REST API Endpoints

**Coming Soon:** Comprehensive API documentation

**Current Endpoints:**

**Admin - Materialized Views**
- `POST /api/admin/materialized-views/refresh/all` - Refresh all views
- `POST /api/admin/materialized-views/refresh/patient-summary` - Refresh patient summary
- `POST /api/admin/materialized-views/refresh/billing-summary` - Refresh billing summary
- `POST /api/admin/materialized-views/refresh/notification-summary` - Refresh notification summary
- `GET /api/admin/materialized-views/health` - Health check

**Future Endpoints:**
- Patient management
- Appointment scheduling
- Billing operations
- Lab test management
- Inventory control
- User management

### API Documentation

Once the application is running, access interactive API documentation:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

---

## 🚢 Deployment

### Development Environment

```bash
# Start PostgreSQL
docker run -d --name clinic-postgres -e POSTGRES_USER=clinic_user -e POSTGRES_DB=clinic -p 5432:5432 postgres:16

# Run application
./gradlew :clinic-backend:bootRun
```

### Production Environment (Future)

**Coming Soon:**
- Docker Compose configuration
- Kubernetes deployment manifests
- CI/CD pipeline (GitHub Actions)
- Environment-specific configurations
- Monitoring and logging setup

---

## 📖 Additional Documentation

### Database

- **[Setup Guide](database/setup.md)** - PostgreSQL installation and configuration
- **[Migrations Guide](database/migrations.md)** - Flyway migration best practices
- **[Materialized Views](database/materialized-views/)** - Performance optimization

### Architecture (Coming Soon)

- System architecture overview
- Multi-tenancy design
- Security architecture
- Performance optimization strategies

### API (Coming Soon)

- API design guidelines
- Authentication and authorization
- Rate limiting and throttling
- API versioning strategy

---

## 🎯 Key Features

### Implemented ✅

**Infrastructure & Configuration**
- ✅ Docker Compose setup (PostgreSQL, Redis, MinIO, RabbitMQ, Prometheus, Grafana, ELK)
- ✅ Multi-module Gradle project (backend, common, migrations)
- ✅ Spring Boot 3.3.7 application configuration
- ✅ Environment-based configuration (dev, prod profiles)

**Database Layer**
- ✅ Database Schema - 23 core tables with Row Level Security
- ✅ Database Migrations - 6 Flyway migrations
- ✅ Materialized Views - 3 views for expensive aggregations
- ✅ CQRS Read Views - 26 database views for optimized queries
- ✅ Custom Enums - 19 type-safe domain enums
- ✅ Partitioned Audit Logs - Monthly partitions for 7-year retention
- ✅ Soft Delete Pattern - All tables support logical deletion

**Application Layer**
- ✅ JPA Entities - 25 entity classes organized by domain
- ✅ Service Layer - 22 service classes with business logic
- ✅ Repository Layer - 23 repositories (JPA + view repositories)
- ✅ MapStruct Mappers - 8 DTO mappers
- ✅ DTO Layer - Request/Response/View DTOs

**Security & Authentication**
- ✅ JWT Authentication - Stateless authentication with refresh tokens
- ✅ Spring Security Configuration - Method-level security enabled
- ✅ Multi-Tenant Context - ThreadLocal tenant management
- ✅ Password Encryption - BCrypt with cost factor 12
- ✅ CORS Configuration - Production-ready CORS setup

**Caching**
- ✅ Redis Distributed Cache - 7 cache regions configured
- ✅ Tenant-Aware Cache Keys - Automatic tenant ID prefixing
- ✅ TTL Configuration - Per-cache TTL based on data volatility
- ✅ Cache Invalidation - Automatic eviction on write operations
- ✅ Cache Statistics - Monitoring via Spring Boot Actuator

**API & Documentation**
- ✅ Admin REST APIs - Materialized view management
- ✅ SpringDoc OpenAPI - Swagger UI configuration
- ✅ Actuator Endpoints - Health checks, metrics, Prometheus
- ✅ Comprehensive Documentation - ARCHITECTURE, SECURITY, CACHING, API guides

**Monitoring & Observability**
- ✅ Prometheus Integration - Metrics collection
- ✅ Grafana Setup - Dashboard visualization
- ✅ ELK Stack - Centralized logging (Elasticsearch, Logstash, Kibana)
- ✅ Health Checks - Spring Boot Actuator endpoints

### In Progress 🚧

**REST Controllers** - Domain-specific endpoints (patients, appointments, billing)
**User Management APIs** - User CRUD, role assignment, profile management

### Planned 📋

**Frontend Development**
- React + TypeScript + Vite
- TailwindCSS + shadcn/ui
- TanStack Query + Zustand
- Authentication flow

**Additional Features**
- Email notifications (RabbitMQ integration)
- Document upload/download (MinIO integration)
- Reporting & analytics dashboards
- Advanced search capabilities
- Real-time updates (WebSocket)

---

## 🛠️ Development

### Build Commands

```bash
# Clean build
./gradlew clean build

# Build without tests
./gradlew clean build -x test

# Run tests
./gradlew test

# Run application
./gradlew :clinic-backend:bootRun

# Check dependencies
./gradlew dependencies
```

### Code Quality

```bash
# Format code (if configured)
./gradlew spotlessApply

# Static analysis (if configured)
./gradlew checkstyleMain

# Generate coverage report
./gradlew jacocoTestReport
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

---

## 🤝 Contributing

### Development Workflow

1. **Create feature branch**
   ```bash
   git checkout -b feature/patient-registration
   ```

2. **Make changes**
   - Write code following project conventions
   - Add tests for new functionality
   - Update documentation

3. **Test changes**
   ```bash
   ./gradlew test
   ./gradlew :clinic-backend:bootRun
   ```

4. **Commit changes**
   ```bash
   git add .
   git commit -m "feat(patients): add patient registration endpoint"
   ```

5. **Push and create PR**
   ```bash
   git push origin feature/patient-registration
   ```

### Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation only
- `style` - Code style (formatting, missing semicolons, etc.)
- `refactor` - Code refactoring
- `test` - Adding tests
- `chore` - Maintenance tasks

**Examples:**
```
feat(patients): add patient registration API
fix(billing): correct invoice total calculation
docs(database): add materialized views documentation
refactor(services): simplify patient service logic
```

### Code Style

- **Java:** Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- **SQL:** Uppercase keywords, snake_case for identifiers
- **Documentation:** Markdown with clear headers and examples

---

## 📞 Support

### Getting Help

- **Documentation:** Start with this README and linked guides
- **Issues:** Report bugs or request features via GitHub Issues
- **Questions:** Open a discussion on GitHub Discussions

### Useful Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/3.3.7/reference/htmlsingle/)
- [PostgreSQL 16 Documentation](https://www.postgresql.org/docs/16/)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [MapStruct Documentation](https://mapstruct.org/documentation/stable/reference/html/)

---

## 📄 License

[Add license information here]

---

## 🗺️ Roadmap

### Phase 1: Foundation (Current) ✅
- ✅ Database schema design and implementation
- ✅ Database migrations setup
- ✅ Materialized views (Phase 1)
- ✅ Service layer structure
- ✅ Basic REST endpoints

### Phase 2: Core Features (Next)
- 🚧 Complete REST API implementation
- 🚧 Authentication & authorization (JWT)
- 🚧 Patient management CRUD
- 🚧 Appointment scheduling
- 🚧 Basic billing

### Phase 3: Advanced Features
- 📋 Lab test management
- 📋 Inventory control
- 📋 Reporting & analytics
- 📋 Notification system
- 📋 Document management

### Phase 4: Optimization & Scale
- 📋 Materialized views (Phases 2-4)
- 📋 Caching strategy (Redis)
- 📋 Performance tuning
- 📋 Load testing
- 📋 Production deployment

---

## 📊 Current Status

**Last Updated:** January 16, 2026

| Component | Status | Progress | Details |
|-----------|--------|----------|---------|
| **Infrastructure** | ✅ Complete | 100% | Docker Compose, PostgreSQL, Redis, MinIO, RabbitMQ, ELK |
| **Database Schema** | ✅ Complete | 100% | 23 core tables with RLS |
| **Database Migrations** | ✅ Complete | 100% | 6 Flyway migrations |
| **Materialized Views** | ✅ Complete | 100% | 3 views for aggregations |
| **CQRS Read Views** | ✅ Complete | 100% | 26 views for optimized queries |
| **Entity Classes** | ✅ Complete | 100% | 25 JPA entities |
| **Service Layer** | ✅ Complete | 100% | 22 service classes |
| **Repository Layer** | ✅ Complete | 100% | 23 repositories |
| **MapStruct Mappers** | ✅ Complete | 100% | 8 DTO mappers |
| **JWT Authentication** | ✅ Complete | 100% | Access + refresh tokens |
| **Spring Security** | ✅ Complete | 100% | Method-level security enabled |
| **Redis Caching** | ✅ Complete | 100% | 7 cache regions configured |
| **Multi-Tenancy** | ✅ Complete | 100% | RLS + tenant context |
| **Audit Logging** | ✅ Complete | 100% | Partitioned, 7-year retention |
| **REST Controllers** | 🚧 In Progress | 15% | Admin APIs complete |
| **API Documentation** | ✅ Complete | 100% | Swagger UI + comprehensive docs |
| **Monitoring** | ✅ Complete | 100% | Prometheus, Grafana, ELK |
| **Documentation** | ✅ Complete | 100% | ARCHITECTURE, SECURITY, CACHING, API |
| **Testing** | 📋 Planned | 0% | Unit, integration, API tests |
| **Frontend** | 📋 Planned | 0% | React + TypeScript + Vite |

---

**Happy coding! 🚀**

For questions or contributions, please refer to the [Contributing](#contributing) section or open an issue on GitHub.
