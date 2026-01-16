# Documentation

Complete technical documentation for the clinic management system.

## Core Architecture & Design

### [Architecture](./ARCHITECTURE.md)
System architecture overview including multi-tenancy design, CQRS pattern implementation, and component interactions. Covers database schema design, service layer patterns, and deployment topology.

### [Mathematical Foundations](./MATHEMATICAL_FOUNDATIONS.md)
Theoretical foundations of the system including:
- Operations Research principles (Queuing Theory, Economic Order Quantity)
- Statistical Process Control (3-Sigma Rule, anomaly detection)
- Discrete Mathematics (graph partitioning for multi-tenancy, append-only audit logs)
- Optimization theorems and invariant enforcement

### [Project Specification](./PROJECT_SPECIFICATION.md)
Complete technical specification of all features, phases, and implementation details. Includes functional requirements, non-functional requirements, and comprehensive feature breakdown.

## API & Integration

### [API Documentation](./API.md)
REST API reference with endpoint documentation, request/response formats, authentication, and usage examples. Covers all 12 controller endpoints with sample payloads.

### [Database Setup](./database/setup.md)
PostgreSQL configuration, connection details, and database initialization guide.

### [Database Migrations](./database/migrations.md)
Flyway migration versioning strategy and migration documentation for all 19 database schema changes.

## Security & Compliance

### [Security](./SECURITY.md)
Security implementation including:
- Authentication & Authorization (JWT, RBAC)
- Data Protection (encryption, XSS/CSRF prevention)
- Access Control (multi-tenancy isolation)
- Regulatory compliance (DPDP Act 2023, IT Act 2000, ABDM Guidelines)

### [Caching Strategy](./CACHING.md)
Distributed Redis caching implementation with:
- 7 cache regions with tenant-aware keys
- TTL-based eviction strategy
- Cache invalidation patterns
- Performance benchmarks

## Infrastructure & Deployment

### [VM Specifications & Costing](./VM_SPECS_AND_COSTING.md)
Production deployment guide with:
- Virtual machine specifications
- Infrastructure sizing recommendations
- Cost analysis for GCP deployment
- Performance scaling guidelines

## Reference

### [Database Materialized Views](./database/materialized-views/)
Performance optimization documentation for:
- 3 materialized views for expensive aggregations
- 90-95% query performance improvement
- Refresh strategies

### [CQRS Read Views](./database/read-views.md)
Optimization documentation for:
- 26 database read views
- Query performance improvements
- Data consistency patterns

---

## Quick Navigation

**For Developers:**
- Start with [Architecture](./ARCHITECTURE.md)
- Reference [API Documentation](./API.md) for endpoints
- Review [Mathematical Foundations](./MATHEMATICAL_FOUNDATIONS.md) for design principles
- Check [Security](./SECURITY.md) for authentication/authorization

**For Architects:**
- Read [Project Specification](./PROJECT_SPECIFICATION.md)
- Review [Architecture](./ARCHITECTURE.md) for system design
- Study [Mathematical Foundations](./MATHEMATICAL_FOUNDATIONS.md)
- Check [VM Specifications](./VM_SPECS_AND_COSTING.md) for deployment

**For Database Administrators:**
- Review [Database Setup](./database/setup.md)
- Study [Database Migrations](./database/migrations.md)
- Check [Materialized Views](./database/materialized-views/)
- Review [CQRS Read Views](./database/read-views.md)

**For Quality & Compliance:**
- Review [Security](./SECURITY.md) compliance section
- Study [Mathematical Foundations](./MATHEMATICAL_FOUNDATIONS.md) for audit completeness
- Check [Project Specification](./PROJECT_SPECIFICATION.md) for compliance features

---

**Technology Stack:** Java 21 • Spring Boot 3.3.7 • PostgreSQL 16 • Redis 7 • Docker

**Last Updated:** January 2025
