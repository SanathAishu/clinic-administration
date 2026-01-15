# Clinic Administration System

A comprehensive, multi-tenant clinic management system for the Indian healthcare market with DPDP Act 2023 compliance, ABDM integration, and production-ready architecture.

## 📚 Documentation

**Complete documentation is available in the [`docs/`](docs/) directory:**

- **[Getting Started Guide](docs/README.md)** - Start here for comprehensive documentation
- **[Database Setup](docs/database/setup.md)** - PostgreSQL configuration and access
- **[Database Migrations](docs/database/migrations.md)** - Flyway migration guide
- **[Materialized Views](docs/database/materialized-views/)** - Performance optimization (Phase 1 complete ✅)
- **[Read Views (CQRS)](docs/database/read-views.md)** - 26 database views for optimized READ operations ✅

---

## Features

- Multi-tenant SaaS architecture with Row Level Security (RLS)
- **Materialized Views** - Phase 1 complete with 90-95% query performance improvement ✅
- **CQRS Read Views** - 26 database views for optimized READ operations ✅
- DPDP Act 2023, IT Act 2000, and ABDM compliance
- ABHA ID integration (Ayushman Bharat Health Account)
- Comprehensive patient care workflow
- Inventory and billing management
- Role-based access control (RBAC) with fine-grained permissions
- JWT-based authentication with session management
- Audit logging with 7-year retention (partitioned by month)
- Real-time notifications
- Document management with MinIO (S3-compatible)
- Monitoring with Prometheus & Grafana
- Centralized logging with ELK Stack
- Automated scheduled refresh for materialized views

## Technology Stack

### Backend
- Java 21 LTS
- Spring Boot 3.3.7
- Spring Security 6 (JWT)
- Spring Data JPA + Hibernate 6.4+
- PostgreSQL 16 with Row Level Security
- Flyway migrations
- MapStruct for DTOs

### Infrastructure
- Docker & Docker Compose
- PostgreSQL 16
- Redis 7
- MinIO (S3-compatible storage)
- RabbitMQ
- Prometheus + Grafana
- ELK Stack (Elasticsearch, Logstash, Kibana)

## Project Structure

```
clinic-administration/
├── clinic-backend/              # Spring Boot application
│   ├── controller/              # REST API endpoints (1 controller)
│   ├── service/                 # Business logic (22 services)
│   ├── repository/              # Data access layer (23 repositories)
│   ├── mapper/                  # MapStruct DTO mappers (8 mappers)
│   └── security/                # JWT authentication
├── clinic-common/               # Shared library
│   ├── entity/                  # JPA entities (25 entities)
│   │   ├── core/                # Tenant, User, Role, Permission, Session, AuditLog
│   │   ├── patient/             # Patient, Vital, Diagnosis, PatientDocument
│   │   ├── clinical/            # Appointment, MedicalRecord, Prescription, LabTest, etc.
│   │   └── operational/         # Billing, Inventory, Notification, StaffSchedule
│   ├── dto/                     # Request/Response DTOs
│   └── security/                # TenantContext
├── clinic-migrations/           # Flyway database migrations (6 migrations)
│   └── db/migration/
│       ├── V1-V4                # Core schema (23 tables)
│       ├── V5                   # Materialized views (3 views)
│       └── V6                   # Read views - CQRS (26 views)
├── docs/                        # Project documentation
└── docker-compose.yml           # Local development environment
```

## Getting Started

### Prerequisites

- Java 21 LTS
- Docker & Docker Compose
- Gradle 8.5+

### Quick Start

1. Clone the repository:
```bash
git clone https://github.com/SanathAishu/clinic-administration.git
cd clinic-administration
```

2. Copy environment variables:
```bash
cp .env.example .env
# Edit .env with your configuration
```

3. Start infrastructure services:
```bash
docker compose up -d
```

This will start:
- PostgreSQL (port 5432)
- Redis (port 6379)
- MinIO (port 9000, console 9001)
- RabbitMQ (port 5672, management 15672)
- Prometheus (port 9090)
- Grafana (port 3001)
- Elasticsearch (port 9200)
- Logstash (port 5000)
- Kibana (port 5601)

4. Run database migrations:
```bash
cd clinic-migrations
../gradlew flywayMigrate
```

5. Build and run the application:
```bash
cd ../clinic-backend
../gradlew bootRun
```

The application will be available at http://localhost:8080

### API Documentation

Once the application is running, access the API documentation at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api/docs

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

### Build

```bash
./gradlew build
```

### Run Tests

```bash
./gradlew test
```

### Database Migrations

Create new migration:
```bash
cd clinic-migrations/src/main/resources/db/migration
# Create V{version}__{description}.sql
```

Apply migrations:
```bash
./gradlew flywayMigrate
```

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
