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
├── clinic-backend/          # Main application (Spring Boot)
│   ├── controller/          # REST endpoints
│   ├── service/             # Business logic
│   ├── repository/          # Data access (JPA)
│   ├── security/            # Authentication & authorization
│   └── config/              # Spring configuration
│
├── clinic-common/           # Shared library
│   ├── entity/              # JPA entities (23 entities)
│   ├── dto/                 # Data transfer objects
│   ├── enums/               # Enumeration types
│   └── mapper/              # MapStruct mappers
│
├── clinic-migrations/       # Database migrations
│   └── db/migration/        # Flyway SQL scripts
│       ├── V1__create_base_schema.sql
│       ├── V2__create_enums.sql
│       ├── V3__create_core_tables.sql
│       ├── V4__create_indexes_and_constraints.sql
│       ├── V5__create_materialized_views_phase1.sql
│       └── V6__create_read_views.sql
│
└── docs/                    # Documentation (this directory)
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

### Implemented

✅ **Database Schema** - 23 core tables with RLS
✅ **Database Migrations** - Flyway version control (5 migrations)
✅ **Materialized Views** - Phase 1 (3 high-impact views)
✅ **Multi-Tenancy** - Row-Level Security policies
✅ **Soft Delete** - Logical deletion support
✅ **Audit Trail** - Partitioned audit logs
✅ **Custom Enums** - Type-safe domain values (10+ enums)
✅ **JSONB Support** - Flexible metadata storage
✅ **Scheduled Tasks** - Automated view refresh
✅ **Admin APIs** - Materialized view management

### In Progress

🚧 **Service Layer** - 22 services created, implementation in progress
🚧 **REST Controllers** - Basic endpoints, expanding coverage
🚧 **Authentication** - JWT-based auth (setup pending)
🚧 **API Documentation** - Swagger/OpenAPI (partial)

### Planned

📋 **User Registration & Login**
📋 **Patient Management**
📋 **Appointment Scheduling**
📋 **Billing & Payments**
📋 **Lab Test Management**
📋 **Inventory Control**
📋 **Reporting & Analytics**
📋 **Notification System**

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

**Last Updated:** January 15, 2026

| Component | Status | Progress |
|-----------|--------|----------|
| Database Schema | ✅ Complete | 100% |
| Database Migrations | ✅ Complete | 100% |
| Materialized Views Phase 1 | ✅ Complete | 100% |
| Entity Classes | ✅ Complete | 100% |
| Service Layer | 🚧 In Progress | 60% |
| REST Controllers | 🚧 In Progress | 20% |
| Authentication | 📋 Planned | 0% |
| Testing | 🚧 In Progress | 30% |
| Documentation | 🚧 In Progress | 70% |

---

**Happy coding! 🚀**

For questions or contributions, please refer to the [Contributing](#contributing) section or open an issue on GitHub.
