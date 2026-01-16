# Documentation

Technical documentation for the Clinic Management System.

## Quick Start

| Role | Start Here |
|------|------------|
| **Developers** | [Architecture](./ARCHITECTURE.md) |
| **API Integration** | [API Reference](./API.md) |
| **Database Admins** | [Database Setup](./database/setup.md) |
| **Security Review** | [Security](./SECURITY.md) |
| **Planning** | [Roadmap](./ROADMAP.md) |
| **Students/Learners** | [Mathematical Foundations - Simple](./MATHEMATICAL_FOUNDATIONS_SIMPLE.md) |

---

## Core Documentation

### [Architecture](./ARCHITECTURE.md)
System architecture overview including:
- Multi-tenancy design with Row Level Security
- CQRS pattern implementation
- Component interactions and data flow
- Deployment topology

### [API Reference](./API.md)
REST API documentation with:
- 12 controller endpoints
- Request/response formats
- Authentication requirements
- Usage examples

### [Security](./SECURITY.md)
Security implementation covering:
- JWT authentication and RBAC authorization
- Data protection and encryption
- Multi-tenancy isolation
- Regulatory compliance (DPDP Act 2023, IT Act 2000)

### [Caching Strategy](./CACHING.md)
Distributed caching with Redis:
- 7 cache regions with tenant-aware keys
- TTL-based eviction strategy
- Cache invalidation patterns
- Performance benchmarks

### [Mathematical Foundations](./MATHEMATICAL_FOUNDATIONS.md)
Theoretical foundations including:
- Operations Research (Queuing Theory, EOQ)
- Statistical Process Control (3-Sigma Rule)
- Discrete Mathematics principles
- Optimization theorems

### [Mathematical Foundations - Simple](./MATHEMATICAL_FOUNDATIONS_SIMPLE.md)
Layman explanations with everyday examples:
- Queuing theory explained via restaurant queues
- EOQ via household shopping decisions
- 3-Sigma rule via exam scores
- Caching via tiffin boxes

---

## Database Documentation

### [Database Setup](./database/setup.md)
PostgreSQL configuration and initialization guide.

### [Database Migrations](./database/migrations.md)
Flyway migration versioning and schema documentation (19 migrations).

### [Read Views](./database/read-views.md)
CQRS optimization with 26 database views.

### [Materialized Views](./database/materialized-views/)
Performance optimization documentation:
- [Design](./database/materialized-views/design.md) - View design and query optimization
- [Refresh Strategies](./database/materialized-views/refresh-strategies.md) - Refresh scheduling
- [Phase 1 Implementation](./database/materialized-views/phase1-implementation.md) - Implementation details

---

## Infrastructure

### [Deployment](./DEPLOYMENT.md)
Production deployment guide with:
- VM specifications and sizing
- Infrastructure recommendations
- Cost analysis for cloud deployment
- Performance scaling guidelines

### [Roadmap](./ROADMAP.md)
Development roadmap for future phases:
- Phase F: Advanced Analytics (3-4 weeks)
- Phase G: Performance Optimization (2-3 weeks)
- Phase H: Security Hardening (3-4 weeks)
- Phase I: Disaster Recovery (2-3 weeks)
- Phase J: User Features & Real-Time (3-4 weeks)

---

## Project Overview

### [Project Specification](./PROJECT_SPECIFICATION.md)
Complete technical specification including:
- Feature requirements
- Technology stack details
- Implementation patterns
- Database schema design

---

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Runtime | Java | 21 LTS |
| Framework | Spring Boot | 3.3.7 |
| Database | PostgreSQL | 16 |
| Cache | Redis | 7 |
| Build | Gradle | 8.x |
| Migrations | Flyway | 10.x |

---

## Document Index

```
docs/
├── README.md                      This file
├── ARCHITECTURE.md                System architecture
├── API.md                         REST API reference
├── SECURITY.md                    Security implementation
├── CACHING.md                     Caching strategy
├── MATHEMATICAL_FOUNDATIONS.md         Theoretical foundations
├── MATHEMATICAL_FOUNDATIONS_SIMPLE.md  Layman explanations
├── PROJECT_SPECIFICATION.md            Complete specification
├── DEPLOYMENT.md                  Infrastructure & deployment
├── ROADMAP.md                     Development roadmap
└── database/
    ├── setup.md                   Database setup
    ├── migrations.md              Migration documentation
    ├── read-views.md              CQRS views
    └── materialized-views/
        ├── README.md              Overview
        ├── design.md              View design
        ├── refresh-strategies.md  Refresh strategies
        └── phase1-implementation.md Implementation
```

---

**Last Updated**: January 2025

