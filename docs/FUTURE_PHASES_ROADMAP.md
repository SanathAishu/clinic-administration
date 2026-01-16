# Future Phases Roadmap: Advanced Features & Enterprise Hardening

## Overview

This document outlines the planned implementation roadmap for phases beyond Phase E (Quality Assurance & Compliance). Each phase builds upon the solid foundation established in earlier phases while addressing specific business needs and technical requirements aligned with ISO 27001 and healthcare industry standards.

Current Status: Phase E (ISO 27001 Compliance) - 94% Complete
Next: Phase F (Advanced Analytics & Reporting)

---

## Phase F: Advanced Analytics & Reporting

**Duration**: 3-4 weeks | **Priority**: HIGH | **Business Value**: Revenue Intelligence

### Objectives

1. Provide deep operational analytics for business intelligence
2. Enable predictive insights for resource planning
3. Generate executive dashboards for decision-making
4. Support regulatory reporting (DPDP Act, Clinical Establishments Act)

### Mathematical Foundation: Time Series Analysis & Forecasting

**Moving Average Smoothing**:
```
SMA(n) = (P₁ + P₂ + ... + Pₙ) / n
```

**Exponential Smoothing** (for trend analysis):
```
Sₜ = α·Pₜ + (1-α)·Sₜ₋₁
```
where α ∈ [0.1, 0.3] for healthcare metrics

**Quarterly Cohort Analysis**:
```
Retention Rate = (Customers at End of Period - New Customers) / Customers at Start × 100%
```

### Features

#### F1: Operational Dashboards

**Real-time Operational Metrics**:
- Appointment utilization by doctor and time slot
- Queue depth and wait time trends (with SMA smoothing)
- Patient throughput and daily scheduling efficiency
- Revenue per appointment type
- No-show rates by patient demographic

**Entities**:
```java
@Entity
@Table(name = "operational_analytics")
public class OperationalAnalytics extends TenantAwareEntity {
    @Column(name = "metric_date")
    private LocalDate metricDate;

    @Column(name = "doctor_id")
    private UUID doctorId;

    @Column(name = "appointment_count")
    private Integer appointmentCount;

    @Column(name = "total_wait_time_minutes")
    private Long totalWaitTimeMinutes;

    @Column(name = "revenue_collected")
    private BigDecimal revenueCollected;

    @Column(name = "no_show_count")
    private Integer noShowCount;

    @Column(name = "utilization_percentage")
    private Double utilizationPercentage; // (actual_minutes / scheduled_minutes) × 100

    @Column(name = "moving_avg_wait_time")
    private Double movingAverageWaitTime; // 7-day SMA

    @Column(name = "forecast_next_week")
    private Integer forecastedAppointments;
}

@Entity
@Table(name = "patient_cohort_analysis")
public class PatientCohortAnalytics extends TenantAwareEntity {
    @Column(name = "cohort_month")
    private YearMonth cohortMonth;

    @Column(name = "cohort_age")
    private Integer ageMonths;

    @Column(name = "patients_in_cohort")
    private Integer patientsInCohort;

    @Column(name = "active_patients")
    private Integer activePatientsThisMonth;

    @Column(name = "retention_rate")
    private Double retentionRate; // (active / initial) × 100

    @Column(name = "revenue_cohort")
    private BigDecimal totalRevenue;
}
```

**Database Migrations**:
```sql
-- V20__create_operational_analytics.sql
CREATE TABLE operational_analytics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    metric_date DATE NOT NULL,
    doctor_id UUID REFERENCES users(id),

    appointment_count INTEGER DEFAULT 0,
    total_wait_time_minutes BIGINT DEFAULT 0,
    revenue_collected NUMERIC(10,2),
    no_show_count INTEGER DEFAULT 0,

    utilization_percentage NUMERIC(5,2),
    moving_avg_wait_time NUMERIC(8,2),
    forecast_next_week INTEGER,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT operational_analytics_date_unique UNIQUE (tenant_id, metric_date, doctor_id)
);

CREATE INDEX idx_operational_analytics_doctor ON operational_analytics(doctor_id, metric_date DESC);
CREATE INDEX idx_operational_analytics_utilization ON operational_analytics(utilization_percentage DESC)
    WHERE utilization_percentage < 70;

-- V21__create_patient_cohort_analysis.sql
CREATE TABLE patient_cohort_analysis (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    cohort_month DATE NOT NULL,
    cohort_age INTEGER NOT NULL,

    patients_in_cohort INTEGER DEFAULT 0,
    active_patients_this_month INTEGER DEFAULT 0,
    retention_rate NUMERIC(5,2),
    total_revenue NUMERIC(12,2),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT patient_cohort_unique UNIQUE (tenant_id, cohort_month, cohort_age)
);

CREATE INDEX idx_cohort_retention ON patient_cohort_analysis(retention_rate DESC);
```

**Service Layer**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OperationalAnalyticsService {

    private final OperationalAnalyticsRepository analyticsRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillingRepository billingRepository;

    @Scheduled(cron = "0 30 1 * * *")  // 1:30 AM daily
    @Transactional
    public void calculateDailyOperationalMetrics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // Query appointments from yesterday
        List<Appointment> appointments = appointmentRepository.findByDateAndStatus(yesterday, AppointmentStatus.COMPLETED);

        // Group by doctor
        Map<UUID, List<Appointment>> byDoctor = appointments.stream()
            .collect(Collectors.groupingBy(Appointment::getDoctorId));

        for (Map.Entry<UUID, List<Appointment>> entry : byDoctor.entrySet()) {
            UUID doctorId = entry.getKey();
            List<Appointment> doctorAppointments = entry.getValue();

            // Calculate metrics
            int appointmentCount = doctorAppointments.size();
            long totalWaitTime = doctorAppointments.stream()
                .mapToLong(a -> Duration.between(a.getScheduledStart(), a.getActualStart()).toMinutes())
                .sum();

            double avgWaitTime = appointmentCount > 0 ? (double) totalWaitTime / appointmentCount : 0;
            double utilizationPercentage = calculateUtilization(doctorId, yesterday);
            double movingAverage = calculateMovingAverage(doctorId, 7);

            // Forecast next week using exponential smoothing
            int forecast = forecastAppointments(doctorId, 7);

            OperationalAnalytics metrics = OperationalAnalytics.builder()
                .metricDate(yesterday)
                .doctorId(doctorId)
                .appointmentCount(appointmentCount)
                .totalWaitTimeMinutes(totalWaitTime)
                .utilizationPercentage(utilizationPercentage)
                .movingAverageWaitTime(movingAverage)
                .forecastedAppointments(forecast)
                .build();

            analyticsRepository.save(metrics);
        }

        log.info("Calculated operational analytics for {}", yesterday);
    }

    private double calculateMovingAverage(UUID doctorId, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        List<OperationalAnalytics> history = analyticsRepository
            .findByDoctorAndDateRange(doctorId, startDate, LocalDate.now());

        return history.stream()
            .mapToDouble(a -> a.getTotalWaitTimeMinutes() / (double) a.getAppointmentCount())
            .average()
            .orElse(0.0);
    }

    private int forecastAppointments(UUID doctorId, int daysAhead) {
        // Exponential smoothing forecast
        List<OperationalAnalytics> history = analyticsRepository.findRecentByDoctor(doctorId, 30);

        if (history.isEmpty()) return 0;

        double alpha = 0.2;  // Smoothing factor
        double smoothed = history.get(0).getAppointmentCount();

        for (int i = 1; i < history.size(); i++) {
            smoothed = alpha * history.get(i).getAppointmentCount() + (1 - alpha) * smoothed;
        }

        return (int) Math.round(smoothed);
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientCohortAnalyticsService {

    private final PatientCohortAnalyticsRepository cohortRepository;
    private final PatientRepository patientRepository;

    @Scheduled(cron = "0 0 2 1 * *")  // 2:00 AM on first day of month
    @Transactional
    public void calculateMonthlyCohortAnalytics() {
        YearMonth currentMonth = YearMonth.now();

        // Get all patients who joined 1, 2, 3, ... 12 months ago
        for (int ageMonths = 1; ageMonths <= 12; ageMonths++) {
            YearMonth cohortMonth = currentMonth.minusMonths(ageMonths);

            // Find patients who registered in cohort month
            LocalDate cohortStart = cohortMonth.atDay(1);
            LocalDate cohortEnd = cohortMonth.atEndOfMonth();

            List<Patient> cohortsPatients = patientRepository
                .findByRegistrationDateBetween(cohortStart, cohortEnd);

            if (cohortsPatients.isEmpty()) continue;

            // Count active patients this month
            int activeCount = cohortsPatients.stream()
                .filter(p -> isActiveThisMonth(p, currentMonth))
                .collect(Collectors.toList())
                .size();

            double retentionRate = (double) activeCount / cohortsPatients.size() * 100;

            BigDecimal cohortRevenue = calculateCohortRevenue(cohortsPatients);

            PatientCohortAnalytics cohort = PatientCohortAnalytics.builder()
                .cohortMonth(cohortMonth)
                .ageMonths(ageMonths)
                .patientsInCohort(cohortsPatients.size())
                .activePatientsThisMonth(activeCount)
                .retentionRate(retentionRate)
                .totalRevenue(cohortRevenue)
                .build();

            cohortRepository.save(cohort);
        }

        log.info("Calculated patient cohort analytics for {}", currentMonth);
    }

    private boolean isActiveThisMonth(Patient patient, YearMonth month) {
        // Check if patient had appointment this month
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        return appointmentRepository.existsForPatientInDateRange(
            patient.getId(), monthStart, monthEnd);
    }
}
```

**API Endpoints** (/api/analytics):
```
GET  /api/analytics/operational/dashboard?start=...&end=... - Dashboard view with key metrics
GET  /api/analytics/operational/doctor/{doctorId} - Doctor-specific analytics
GET  /api/analytics/utilization/low-performers?threshold=70 - Alert low utilization
GET  /api/analytics/forecast/next-week - Weekly appointment forecast
GET  /api/analytics/cohort/retention - Patient retention cohort analysis
GET  /api/analytics/cohort/{cohortMonth} - Specific cohort metrics
GET  /api/analytics/revenue/by-type - Revenue breakdown by appointment type
POST /api/analytics/export/csv?type=operational - Export analytics as CSV
```

---

#### F2: Regulatory Reporting

**Reporting Requirements**:

1. **DPDP Act 2023 Compliance Report**:
   - PII processing audit trail
   - Consent validity tracking
   - Data breach incident log
   - Retention policy adherence

2. **Clinical Establishments Act Reporting**:
   - Doctor availability and consultation hours
   - Patient service quality metrics
   - Complaint resolution turnaround time
   - Infrastructure capability report

3. **Monthly Performance Report**:
   - SLA compliance summary
   - Error rates and incident summary
   - Security posture assessment
   - Archival and retention compliance

**Entities**:
```java
@Entity
@Table(name = "regulatory_reports")
public class RegulatoryReport extends TenantAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type")
    private ReportType reportType;  // DPDP, CLINICAL_ESTABLISHMENTS, MONTHLY_PERFORMANCE

    @Column(name = "report_period_start")
    private LocalDate periodStart;

    @Column(name = "report_period_end")
    private LocalDate periodEnd;

    @Column(name = "report_status")
    @Enumerated(EnumType.STRING)
    private ReportStatus status;  // DRAFT, FINALIZED, SIGNED

    @Column(name = "report_content", columnDefinition = "JSONB")
    private String reportContent;  // JSON structure with metrics

    @Column(name = "generated_by")
    private UUID generatedBy;

    @Column(name = "signed_by")
    private UUID signedBy;

    @Column(name = "digital_signature")
    private String digitalSignature;  // RSA-2048 signature for legal validity
}

enum ReportType {
    DPDP_COMPLIANCE,
    CLINICAL_ESTABLISHMENTS_ACT,
    MONTHLY_PERFORMANCE,
    ANNUAL_AUDIT,
    INCIDENT_REPORT
}

enum ReportStatus {
    DRAFT,
    UNDER_REVIEW,
    FINALIZED,
    SIGNED,
    ARCHIVED
}
```

**Service**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class RegulatoryReportingService {

    private final RegulatoryReportRepository reportRepository;
    private final SensitiveDataAccessLogRepository auditRepository;
    private final ConsentRepository consentRepository;

    public RegulatoryReport generateDPDPComplianceReport(LocalDate startDate, LocalDate endDate, UUID tenantId) {
        // PII Processing Audit Trail
        List<SensitiveDataAccessLog> accessLogs = auditRepository
            .findByDateRangeAndTenant(startDate, endDate, tenantId);

        // Consent Validity Check
        List<Consent> activeConsents = consentRepository
            .findValidConsents(tenantId, LocalDate.now());

        List<Consent> expiredConsents = consentRepository
            .findExpiredConsents(tenantId, startDate, endDate);

        // Build DPDP Report Structure
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("piiProcessingAudit", buildPIIAuditSection(accessLogs));
        reportData.put("consentCompliance", buildConsentComplianceSection(activeConsents, expiredConsents));
        reportData.put("incidentLog", buildIncidentSection(tenantId, startDate, endDate));
        reportData.put("retentionPolicy", buildRetentionPolicySection(tenantId));

        RegulatoryReport report = RegulatoryReport.builder()
            .reportType(ReportType.DPDP_COMPLIANCE)
            .periodStart(startDate)
            .periodEnd(endDate)
            .status(ReportStatus.DRAFT)
            .reportContent(objectToJson(reportData))
            .build();

        return reportRepository.save(report);
    }

    public RegulatoryReport generateClinicalEstablishmentsReport(LocalDate startDate, LocalDate endDate, UUID tenantId) {
        // Doctor Availability Metrics
        List<DoctorAvailability> availability = doctorAvailabilityRepository
            .findByDateRangeAndTenant(startDate, endDate, tenantId);

        // Patient Service Quality
        List<Appointment> appointments = appointmentRepository
            .findByDateRangeAndTenant(startDate, endDate, tenantId);

        Map<String, Object> reportData = new HashMap<>();
        reportData.put("doctorAvailability", calculateDoctorMetrics(availability));
        reportData.put("serviceQuality", calculateServiceQualityMetrics(appointments));
        reportData.put("complaintResolution", calculateComplaintMetrics(tenantId, startDate, endDate));
        reportData.put("infrastructure", buildInfrastructureCapability());

        RegulatoryReport report = RegulatoryReport.builder()
            .reportType(ReportType.CLINICAL_ESTABLISHMENTS_ACT)
            .periodStart(startDate)
            .periodEnd(endDate)
            .status(ReportStatus.DRAFT)
            .reportContent(objectToJson(reportData))
            .build();

        return reportRepository.save(report);
    }

    public void signReport(UUID reportId, String privateKey) throws Exception {
        RegulatoryReport report = reportRepository.findById(reportId)
            .orElseThrow(() -> new EntityNotFoundException("Report not found"));

        // Generate digital signature using RSA-2048
        String signature = generateRSASignature(report.getReportContent(), privateKey);

        report.setStatus(ReportStatus.SIGNED);
        report.setDigitalSignature(signature);
        report.setSignedBy(securityUtils.getCurrentUserId());

        reportRepository.save(report);
        log.info("Report {} signed with RSA-2048 signature", reportId);
    }
}
```

**API Endpoints**:
```
GET  /api/reports/regulatory?type=DPDP&start=...&end=... - Get regulatory report
POST /api/reports/regulatory/generate - Generate new report
PUT  /api/reports/regulatory/{id}/sign - Sign report digitally
GET  /api/reports/regulatory/{id}/verify - Verify signature authenticity
GET  /api/reports/export/{id}/pdf - Export as PDF
POST /api/reports/regulatory/{id}/submit - Submit to regulatory body
```

---

## Phase G: Performance Optimization & Scaling

**Duration**: 2-3 weeks | **Priority**: HIGH | **Business Value**: System Reliability

### Objectives

1. Optimize database query performance
2. Implement advanced caching strategies
3. Enable horizontal scaling for load distribution
4. Reduce API response times to <200ms P95

### Mathematical Foundation: Performance Analysis

**Amdahl's Law** (for parallel optimization):
```
Speedup = 1 / (f + (1-f)/p)
```
where f = fraction of sequential code, p = number of processors

**Little's Law Validation**:
```
L = λ × W
L = average queue length
λ = arrival rate
W = average wait time
```

### Features

#### G1: Query Optimization & Database Tuning

**Analysis Tools**:
- Query plan analysis with EXPLAIN ANALYZE
- Slow query log aggregation
- Index effectiveness metrics

**Optimizations**:
1. Composite indexes on frequently filtered columns
2. Partial indexes for soft-deleted records
3. Materialized views for complex aggregations
4. Query result set pagination with cursor-based navigation

**Entities**:
```java
@Entity
@Table(name = "query_performance_log", indexes = {
    @Index(name = "idx_query_perf_duration", columnList = "execution_time_ms DESC"),
    @Index(name = "idx_query_perf_slow", columnList = "is_slow"),
    @Index(name = "idx_query_perf_timestamp", columnList = "executed_at DESC")
})
public class QueryPerformanceLog extends TenantAwareEntity {

    @Column(name = "query_hash")
    private String queryHash;  // MD5 hash of query for grouping similar queries

    @Column(name = "query_text", columnDefinition = "TEXT")
    private String queryText;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "is_slow")
    private Boolean isSlow;  // true if > 100ms

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "executed_at")
    private Instant executedAt;
}
```

#### G2: Multi-Level Caching Strategy

**Caching Layers**:

1. **L1: Application Cache** (Caffeine)
   - Hot data: Patient info, Doctor availability, Appointment schedules
   - TTL: 5-30 minutes depending on data volatility
   - Size limit: 10,000 entries max

2. **L2: Distributed Cache** (Redis)
   - Session data, User preferences, Authentication tokens
   - TTL: 1-24 hours
   - Shared across all instances

3. **L3: Query Result Cache**
   - Compliance metrics, Regulatory reports (read-heavy)
   - TTL: 1-7 days
   - Invalidation on data modification

**Caching Configuration**:
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new CacheChainManager(
            caffeineCacheManager(),      // L1
            redisCacheManager()          // L2
        );
    }

    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
            "patients",
            "doctors",
            "appointments",
            "prescriptions",
            "billing"
        );

        manager.setCacheSpecification("maximumSize=10000,expireAfterWrite=30m");
        return manager;
    }
}

@Service
public class AppointmentService {

    @Cacheable(value = "appointments", key = "#doctorId + ':' + #date")
    public List<Appointment> getAvailableSlots(UUID doctorId, LocalDate date) {
        // Expensive query - only runs on cache miss
        return appointmentRepository.findAvailableSlots(doctorId, date);
    }

    @CacheEvict(value = "appointments", key = "#appointment.doctorId + ':' + #appointment.date")
    public Appointment createAppointment(Appointment appointment) {
        return appointmentRepository.save(appointment);
    }
}
```

#### G3: Database Partitioning & Archival Strategy

**Time-Based Partitioning**:
```sql
-- Partition sensitive_data_access_log by month
CREATE TABLE sensitive_data_access_log (
    id UUID,
    tenant_id UUID,
    user_id UUID,
    patient_id UUID,
    access_timestamp TIMESTAMPTZ,
    ...
) PARTITION BY RANGE (DATE_TRUNC('month', access_timestamp));

CREATE TABLE sensitive_data_access_log_2025_01 PARTITION OF sensitive_data_access_log
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE sensitive_data_access_log_2025_02 PARTITION OF sensitive_data_access_log
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
```

**Archive Strategy**:
- Keep last 3 months in hot storage
- Archive older partitions to cold storage (S3)
- Maintain schema for archive transparency

#### G4: Load Balancing & Horizontal Scaling

**Deployment Topology**:
```
       Load Balancer (HAProxy/NGINX)
       |                          |
   Instance 1              Instance 2
   (Port 8081)             (Port 8082)
       |                          |
   Embedded Cache         Embedded Cache
   (Caffeine L1)          (Caffeine L1)
       |__________________|________|
              |
          Redis Cluster
          (L2 Cache)
              |
          PostgreSQL
          Primary
              |
              +---------> Read Replicas
```

**Service Registry** (Spring Cloud Eureka):
```java
@EnableEurekaServer
@SpringBootApplication
public class ServiceRegistryApplication {}

@Service
@EnableDiscoveryClient
public class AppointmentService {
    @LoadBalanced
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

---

## Phase H: Security Hardening & Threat Detection

**Duration**: 3-4 weeks | **Priority**: CRITICAL | **Business Value**: Compliance & Trust

### Objectives

1. Implement encryption at rest for sensitive data
2. Enable advanced threat detection
3. Implement rate limiting and DDoS protection
4. Secure API communication with mTLS

### Features

#### H1: Encryption at Rest & Data Protection

**Column-Level Encryption**:
```java
@Entity
@Table(name = "patients")
public class Patient extends TenantAwareEntity {

    @Encrypted  // Custom annotation for field-level encryption
    @Column(name = "phone_number")
    private String phoneNumber;

    @Encrypted
    @Column(name = "email_address")
    private String emailAddress;

    @Encrypted
    @Column(name = "insurance_id")
    private String insuranceId;
}

@Aspect
@Component
public class EncryptionAspect {

    @Before("@annotation(encrypted)")
    public void encryptBeforePersist(JoinPoint joinPoint) {
        // Encrypt using AES-256-GCM before database write
    }

    @After("@annotation(encrypted)")
    public void decryptAfterRetrieval(JoinPoint joinPoint) {
        // Decrypt after database read
    }
}
```

**Database Encryption**:
```sql
-- Enable pgcrypto extension
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Create encrypted columns
ALTER TABLE patients
ADD COLUMN ssn_encrypted BYTEA;

-- Encryption function with key rotation
CREATE OR REPLACE FUNCTION encrypt_ssn(plaintext TEXT, key_id INTEGER)
RETURNS BYTEA AS $$
DECLARE
    key BYTEA;
BEGIN
    SELECT keydata INTO key FROM encryption_keys WHERE id = key_id;
    RETURN pgp_sym_encrypt(plaintext, key);
END;
$$ LANGUAGE plpgsql;

-- Create encryption keys table
CREATE TABLE encryption_keys (
    id INTEGER PRIMARY KEY,
    keydata BYTEA NOT NULL,
    algorithm VARCHAR(50) NOT NULL,
    key_version INTEGER DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    rotated_at TIMESTAMPTZ,
    active BOOLEAN DEFAULT TRUE
);
```

#### H2: Advanced Threat Detection

**Anomaly Detection Engine**:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ThreatDetectionService {

    private final SensitiveDataAccessLogRepository auditRepository;
    private final SecurityIncidentRepository incidentRepository;

    @Scheduled(cron = "0 */15 * * * *")  // Every 15 minutes
    @Transactional
    public void detectAnomalies() {
        Instant lookbackWindow = Instant.now().minus(Duration.ofMinutes(30));
        List<SensitiveDataAccessLog> recentAccess = auditRepository
            .findSinceTimestamp(lookbackWindow);

        // Anomaly Detection Rules
        detectBruteForceAttack(recentAccess);
        detectDataExfiltration(recentAccess);
        detectPrivilegeEscalation(recentAccess);
        detectGeoLocationAnomaly(recentAccess);
    }

    private void detectBruteForceAttack(List<SensitiveDataAccessLog> logs) {
        // Rule: > 50 failed auth attempts from single IP in 5 minutes
        Map<String, List<SensitiveDataAccessLog>> byIp = logs.stream()
            .collect(Collectors.groupingBy(SensitiveDataAccessLog::getIpAddress));

        for (Map.Entry<String, List<SensitiveDataAccessLog>> entry : byIp.entrySet()) {
            List<SensitiveDataAccessLog> ipLogs = entry.getValue();

            if (ipLogs.size() > 50) {
                SecurityIncident incident = SecurityIncident.builder()
                    .incidentType(IncidentType.BRUTE_FORCE_ATTACK)
                    .severity(IncidentSeverity.HIGH)
                    .sourceIpAddress(entry.getKey())
                    .detectedAt(Instant.now())
                    .description(String.format("Brute force detected: %d requests from %s",
                        ipLogs.size(), entry.getKey()))
                    .recommendedAction("Block IP address and notify security team")
                    .build();

                incidentRepository.save(incident);
                alertSecurityTeam(incident);
            }
        }
    }

    private void detectDataExfiltration(List<SensitiveDataAccessLog> logs) {
        // Rule: User exports > 10,000 records in single session
        Map<UUID, Integer> exportCountByUser = logs.stream()
            .filter(SensitiveDataAccessLog::getDataExported)
            .collect(Collectors.groupingBy(
                log -> log.getUser().getId(),
                Collectors.summingInt(log -> 1)
            ));

        for (Map.Entry<UUID, Integer> entry : exportCountByUser.entrySet()) {
            if (entry.getValue() > 10000) {
                SecurityIncident incident = SecurityIncident.builder()
                    .incidentType(IncidentType.DATA_EXFILTRATION)
                    .severity(IncidentSeverity.CRITICAL)
                    .affectedUserId(entry.getKey())
                    .detectedAt(Instant.now())
                    .description(String.format("Suspicious data export: %d records", entry.getValue()))
                    .recommendedAction("Revoke export privileges and investigate user")
                    .build();

                incidentRepository.save(incident);
                alertSecurityTeam(incident);
                auditService.logSecurityEvent(incident);
            }
        }
    }

    private void detectPrivilegeEscalation(List<SensitiveDataAccessLog> logs) {
        // Rule: Non-admin accesses admin-only entity
        for (SensitiveDataAccessLog log : logs) {
            User user = log.getUser();

            if (!user.hasRole(Role.ADMIN) && isAdminOnlyEntity(log.getEntityType())) {
                SecurityIncident incident = SecurityIncident.builder()
                    .incidentType(IncidentType.PRIVILEGE_ESCALATION)
                    .severity(IncidentSeverity.HIGH)
                    .affectedUserId(user.getId())
                    .detectedAt(log.getAccessTimestamp())
                    .description("Unauthorized access to admin entity: " + log.getEntityType())
                    .build();

                incidentRepository.save(incident);
                alertSecurityTeam(incident);
            }
        }
    }

    private void detectGeoLocationAnomaly(List<SensitiveDataAccessLog> logs) {
        // Rule: User access from geographically impossible location
        for (SensitiveDataAccessLog log : logs) {
            String currentIp = log.getIpAddress();
            Location currentLocation = geoIpService.lookupLocation(currentIp);

            List<SensitiveDataAccessLog> previousAccess = auditRepository
                .findRecentAccessByUser(log.getUser().getId(), 10);

            for (SensitiveDataAccessLog prev : previousAccess) {
                Location prevLocation = geoIpService.lookupLocation(prev.getIpAddress());
                long timeDiffSeconds = Duration.between(prev.getAccessTimestamp(), log.getAccessTimestamp())
                    .getSeconds();

                double distanceKm = calculateDistance(currentLocation, prevLocation);
                double speedMps = distanceKm / timeDiffSeconds;

                // Speed of light / physics impossibility check
                if (speedMps > 900) {  // > Mach 2.6 (supersonic)
                    SecurityIncident incident = SecurityIncident.builder()
                        .incidentType(IncidentType.GEO_ANOMALY)
                        .severity(IncidentSeverity.HIGH)
                        .affectedUserId(log.getUser().getId())
                        .detectedAt(log.getAccessTimestamp())
                        .description(String.format("Geographically impossible travel: %.0f km in %d seconds",
                            distanceKm, timeDiffSeconds))
                        .build();

                    incidentRepository.save(incident);
                    alertSecurityTeam(incident);
                }
            }
        }
    }
}
```

**Security Incident Entity**:
```java
@Entity
@Table(name = "security_incidents")
public class SecurityIncident extends TenantAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type")
    private IncidentType incidentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private IncidentSeverity severity;  // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "source_ip_address", columnDefinition = "inet")
    private String sourceIpAddress;

    @Column(name = "affected_user_id")
    private UUID affectedUserId;

    @Column(name = "detected_at")
    private Instant detectedAt;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(name = "response_status")
    @Enumerated(EnumType.STRING)
    private IncidentStatus status;  // OPEN, INVESTIGATING, RESOLVED, FALSE_POSITIVE

    @Column(name = "response_notes", columnDefinition = "TEXT")
    private String responseNotes;

    @Column(name = "responded_by")
    private UUID respondedBy;

    @Column(name = "responded_at")
    private Instant respondedAt;
}

enum IncidentType {
    BRUTE_FORCE_ATTACK,
    DATA_EXFILTRATION,
    PRIVILEGE_ESCALATION,
    GEO_ANOMALY,
    INJECTION_ATTEMPT,
    UNAUTHORIZED_ACCESS,
    SESSION_HIJACKING,
    CREDENTIAL_STUFFING
}

enum IncidentSeverity {
    LOW,      // Monitor
    MEDIUM,   // Alert
    HIGH,     // Notify & Restrict
    CRITICAL  // Block & Escalate
}

enum IncidentStatus {
    OPEN,
    INVESTIGATING,
    RESOLVED,
    FALSE_POSITIVE,
    ESCALATED
}
```

**Database Migration**:
```sql
-- V22__create_security_incidents.sql
CREATE TABLE security_incidents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    incident_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,

    source_ip_address inet,
    affected_user_id UUID REFERENCES users(id),

    detected_at TIMESTAMPTZ NOT NULL,
    description TEXT,
    recommended_action TEXT,

    response_status VARCHAR(20) DEFAULT 'OPEN',
    response_notes TEXT,
    responded_by UUID REFERENCES users(id),
    responded_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT severity_in_enum CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT status_in_enum CHECK (response_status IN ('OPEN', 'INVESTIGATING', 'RESOLVED', 'FALSE_POSITIVE'))
);

CREATE INDEX idx_security_incidents_severity ON security_incidents(severity DESC)
    WHERE response_status = 'OPEN';
CREATE INDEX idx_security_incidents_timestamp ON security_incidents(detected_at DESC);
CREATE INDEX idx_security_incidents_user ON security_incidents(affected_user_id);
```

#### H3: Rate Limiting & DDoS Protection

**Implementation**:
```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientId = extractClientIdentifier(request);
        RateLimitKey key = new RateLimitKey(clientId, request.getRequestURI());

        if (!rateLimiter.allowRequest(key)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded. Retry after " +
                rateLimiter.getRetryAfterSeconds(key) + " seconds");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIdentifier(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final RedisTemplate<String, RateLimitData> redisTemplate;

    public boolean allowRequest(RateLimitKey key) {
        String redisKey = "ratelimit:" + key.getClientId() + ":" + key.getEndpoint();

        RateLimitData data = redisTemplate.opsForValue().get(redisKey);

        if (data == null) {
            data = new RateLimitData(1, Instant.now().plus(Duration.ofMinutes(1)));
            redisTemplate.opsForValue().set(redisKey, data, Duration.ofMinutes(1));
            return true;
        }

        if (data.getRequestCount() < 100) {  // 100 requests per minute
            data.setRequestCount(data.getRequestCount() + 1);
            redisTemplate.opsForValue().set(redisKey, data, Duration.ofMinutes(1));
            return true;
        }

        return false;
    }

    public long getRetryAfterSeconds(RateLimitKey key) {
        String redisKey = "ratelimit:" + key.getClientId() + ":" + key.getEndpoint();
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        return ttl > 0 ? ttl : 60;
    }
}
```

---

## Phase I: Disaster Recovery & Business Continuity

**Duration**: 2-3 weeks | **Priority**: HIGH | **Business Value**: Operational Resilience

### Objectives

1. Implement automated backup strategy
2. Enable point-in-time recovery
3. Provide disaster recovery runbooks
4. Implement active-passive failover

### Features

#### I1: Automated Backup Strategy

**Backup Schedule**:
- **Hourly Snapshots**: Last 24 hours (WAL-based incremental)
- **Daily Full Backups**: Last 30 days
- **Weekly Full Backups**: Last 12 weeks
- **Monthly Archives**: 5-year retention

**Implementation**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private final PostgresBackupExecutor backupExecutor;
    private final S3Service s3Service;
    private final BackupAuditRepository backupAuditRepository;

    @Scheduled(cron = "0 0 * * * *")  // Hourly
    @Transactional
    public void performHourlyBackup() {
        Instant startTime = Instant.now();

        try {
            String backupId = UUID.randomUUID().toString();
            File backupFile = backupExecutor.createWALBackup(backupId);

            // Upload to S3 with encryption
            String s3Path = s3Service.uploadWithEncryption(
                backupFile,
                "backups/hourly/" + LocalDate.now() + "/" + backupId + ".sql.gz"
            );

            // Verify backup integrity
            String checksumMd5 = calculateMd5(backupFile);
            boolean verified = s3Service.verifyChecksum(s3Path, checksumMd5);

            if (!verified) {
                throw new BackupException("Checksum verification failed");
            }

            // Record backup in audit
            BackupAudit audit = BackupAudit.builder()
                .backupId(backupId)
                .backupType(BackupType.HOURLY_INCREMENTAL)
                .startTime(startTime)
                .endTime(Instant.now())
                .sizeBytes(backupFile.length())
                .storageLocation(s3Path)
                .status(BackupStatus.SUCCESS)
                .checksumMd5(checksumMd5)
                .build();

            backupAuditRepository.save(audit);
            log.info("Completed hourly backup {} (size: {} MB)",
                backupId, backupFile.length() / 1024 / 1024);

        } catch (Exception e) {
            BackupAudit audit = BackupAudit.builder()
                .backupType(BackupType.HOURLY_INCREMENTAL)
                .startTime(startTime)
                .endTime(Instant.now())
                .status(BackupStatus.FAILED)
                .errorMessage(e.getMessage())
                .build();
            backupAuditRepository.save(audit);

            alertOpsTeam("Backup failed: " + e.getMessage());
            throw e;
        }
    }

    @Scheduled(cron = "0 0 2 * * *")  // Daily at 2:00 AM
    @Transactional
    public void performDailyFullBackup() {
        // Full database dump with compression
        String backupId = LocalDate.now() + "-full-" + UUID.randomUUID();
        File backupFile = backupExecutor.createFullBackup(backupId);

        String s3Path = s3Service.uploadWithEncryption(
            backupFile,
            "backups/daily/" + LocalDate.now() + "/" + backupId + ".sql.gz"
        );

        // Prune old hourly backups
        LocalDate pruneDate = LocalDate.now().minusDays(1);
        backupAuditRepository.deleteByBackupTypeAndStartTimeBefore(
            BackupType.HOURLY_INCREMENTAL, pruneDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        log.info("Completed daily full backup {}", backupId);
    }
}

@Entity
@Table(name = "backup_audit")
public class BackupAudit extends TenantAwareEntity {

    @Column(name = "backup_id")
    private String backupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "backup_type")
    private BackupType backupType;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "storage_location")
    private String storageLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BackupStatus status;

    @Column(name = "checksum_md5")
    private String checksumMd5;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Transient
    public long getDurationSeconds() {
        if (endTime != null) {
            return Duration.between(startTime, endTime).getSeconds();
        }
        return 0;
    }
}

enum BackupType {
    HOURLY_INCREMENTAL,
    DAILY_FULL,
    WEEKLY_FULL,
    MONTHLY_ARCHIVE
}

enum BackupStatus {
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    VERIFIED
}
```

**Database Migration**:
```sql
-- V23__create_backup_audit.sql
CREATE TABLE backup_audit (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    backup_id VARCHAR(255) NOT NULL UNIQUE,
    backup_type VARCHAR(50) NOT NULL,

    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    size_bytes BIGINT,

    storage_location TEXT,
    status VARCHAR(20) NOT NULL,

    checksum_md5 VARCHAR(32),
    error_message TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT backup_type_enum CHECK (backup_type IN ('HOURLY_INCREMENTAL', 'DAILY_FULL', 'WEEKLY_FULL', 'MONTHLY_ARCHIVE')),
    CONSTRAINT backup_status_enum CHECK (status IN ('IN_PROGRESS', 'SUCCESS', 'FAILED', 'VERIFIED'))
);

CREATE INDEX idx_backup_audit_timestamp ON backup_audit(start_time DESC);
CREATE INDEX idx_backup_audit_status ON backup_audit(status) WHERE status IN ('FAILED', 'IN_PROGRESS');
```

#### I2: Point-in-Time Recovery (PITR)

**Implementation**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PointInTimeRecoveryService {

    private final PostgresRecoveryExecutor recoveryExecutor;
    private final S3Service s3Service;
    private final BackupAuditRepository backupAuditRepository;

    public RecoveryPlan createRecoveryPlan(Instant targetTime, UUID tenantId) {
        // Find full backup before target time
        BackupAudit baseBackup = backupAuditRepository
            .findLatestFullBackupBefore(targetTime, tenantId)
            .orElseThrow(() -> new NoBackupAvailableException("No full backup before " + targetTime));

        // Find all incremental backups between base and target
        List<BackupAudit> incrementalBackups = backupAuditRepository
            .findIncrementalBackupsBetween(baseBackup.getEndTime(), targetTime, tenantId);

        RecoveryPlan plan = RecoveryPlan.builder()
            .recoveryId(UUID.randomUUID())
            .targetTime(targetTime)
            .baseBackupId(baseBackup.getBackupId())
            .incrementalBackupIds(extractIds(incrementalBackups))
            .estimatedDurationMinutes(calculateRecoveryDuration(baseBackup, incrementalBackups))
            .estimatedDataLossSeconds(Duration.between(incrementalBackups.get(incrementalBackups.size() - 1).getEndTime(), targetTime).getSeconds())
            .build();

        return plan;
    }

    public void executeRecovery(RecoveryPlan plan) throws Exception {
        log.info("Starting PITR recovery: {} to {}", plan.getRecoveryId(), plan.getTargetTime());

        // 1. Download base backup from S3
        BackupAudit baseBackup = backupAuditRepository.findByBackupId(plan.getBaseBackupId());
        File baseBackupFile = s3Service.download(baseBackup.getStorageLocation());

        // 2. Restore base backup to temporary standby instance
        String standbyInstanceId = recoveryExecutor.createStandbyInstance();
        recoveryExecutor.restoreBackup(baseBackupFile, standbyInstanceId);

        // 3. Apply incremental backups in sequence
        for (String incrementalBackupId : plan.getIncrementalBackupIds()) {
            BackupAudit incrementalBackup = backupAuditRepository.findByBackupId(incrementalBackupId);
            File incrementalFile = s3Service.download(incrementalBackup.getStorageLocation());
            recoveryExecutor.applyIncrementalBackup(incrementalFile, standbyInstanceId);
        }

        // 4. Replay WAL until target time
        recoveryExecutor.replayWalUntilTime(standbyInstanceId, plan.getTargetTime());

        // 5. Promote standby to primary (DNS switchover)
        String primaryAddress = recoveryExecutor.promoteStandbyToPrimary(standbyInstanceId);
        dnsService.updatePrimaryAddress(primaryAddress);

        log.info("PITR recovery completed successfully at {}", primaryAddress);
    }
}

@Entity
@Table(name = "recovery_plans")
public class RecoveryPlan {

    @Id
    private UUID recoveryId;

    @Column(name = "target_time")
    private Instant targetTime;

    @Column(name = "base_backup_id")
    private String baseBackupId;

    @Column(name = "incremental_backup_ids", columnDefinition = "TEXT[]")
    private String[] incrementalBackupIds;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "estimated_data_loss_seconds")
    private Long estimatedDataLossSeconds;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private RecoveryStatus status;
}
```

---

## Phase J: Advanced User Features & Real-Time Capabilities

**Duration**: 3-4 weeks | **Priority**: MEDIUM | **Business Value**: User Experience

### Objectives

1. Implement real-time notifications
2. Add messaging/chat capabilities
3. Enable document sharing and collaboration
4. Implement mobile app support

### Features

#### J1: Real-Time Notification System

**Implementation using WebSockets + Redis Pub/Sub**:

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic/", "/queue/");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
            .setAllowedOrigins("*")
            .withSockJS();
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Notification> redisTemplate;
    private final NotificationRepository notificationRepository;

    public void notifyUser(UUID userId, Notification notification) {
        // Store in database for persistence
        notificationRepository.save(notification);

        // Send real-time via WebSocket
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/notifications",
            notification
        );

        // Publish to Redis for cluster distribution
        redisTemplate.convertAndSend("notifications:" + userId, notification);

        log.debug("Notification sent to user: {} - {}", userId, notification.getTitle());
    }

    public void notifyMultipleUsers(Set<UUID> userIds, Notification notification) {
        for (UUID userId : userIds) {
            notifyUser(userId, notification);
        }
    }

    public void notifyTenant(UUID tenantId, Notification notification) {
        // Broadcast to all users in tenant
        messagingTemplate.convertAndSend("/topic/tenant/" + tenantId, notification);
    }
}

@Entity
@Table(name = "notifications")
public class Notification extends TenantAwareEntity {

    @Column(name = "recipient_id")
    private UUID recipientId;

    @Column(name = "title")
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private NotificationType type;  // APPOINTMENT_REMINDER, SYSTEM_ALERT, MESSAGE, REPORT_READY

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private NotificationPriority priority;  // LOW, NORMAL, HIGH, URGENT

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}

enum NotificationType {
    APPOINTMENT_REMINDER,
    APPOINTMENT_CANCELLATION,
    DOCTOR_UNAVAILABLE,
    SYSTEM_ALERT,
    MESSAGE,
    REPORT_READY,
    PRESCRIPTION_READY,
    BILLING_INVOICE,
    LAB_RESULT_READY
}

enum NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}
```

**Scheduled Notifications**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderService {

    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 10 * * *")  // 10:00 AM daily
    @Transactional
    public void sendAppointmentReminders() {
        // Send reminders for appointments in next 24 hours
        Instant tomorrow = Instant.now().plus(Duration.ofHours(24));
        Instant today = Instant.now();

        List<Appointment> upcomingAppointments = appointmentRepository
            .findByScheduledTimeBetween(today, tomorrow, AppointmentStatus.CONFIRMED);

        for (Appointment appointment : upcomingAppointments) {
            Notification reminder = Notification.builder()
                .recipientId(appointment.getPatient().getId())
                .title("Appointment Reminder")
                .message(String.format("Your appointment with Dr. %s is tomorrow at %s",
                    appointment.getDoctor().getFullName(),
                    appointment.getScheduledStart().format(DateTimeFormatter.ofPattern("HH:mm"))))
                .type(NotificationType.APPOINTMENT_REMINDER)
                .priority(NotificationPriority.NORMAL)
                .relatedEntityId(appointment.getId())
                .relatedEntityType("Appointment")
                .build();

            notificationService.notifyUser(appointment.getPatient().getId(), reminder);
        }

        log.info("Sent {} appointment reminders", upcomingAppointments.size());
    }
}
```

---

## Implementation Summary Table

| Phase | Duration | Priority | Key Features | Dependencies |
|-------|----------|----------|--------------|--------------|
| **F** | 3-4w | HIGH | Analytics Dashboard, Cohort Analysis, Regulatory Reports | V20-V21 Migrations |
| **G** | 2-3w | HIGH | Query Optimization, Multi-Level Caching, Load Balancing | Redis, Caffeine, HAProxy |
| **H** | 3-4w | CRITICAL | Encryption at Rest, Threat Detection, Rate Limiting | pgcrypto, Security Incident Schema |
| **I** | 2-3w | HIGH | Automated Backups, PITR, Failover Strategy | S3, WAL, PostgreSQL Streaming |
| **J** | 3-4w | MEDIUM | Real-Time Notifications, Messaging, WebSockets | Redis Pub/Sub, WebSocket Server |

---

## Cross-Phase Architecture Principles

### 1. Multi-Tenancy Isolation (MANDATORY ACROSS ALL PHASES)
Every new entity inherits from `TenantAwareEntity` with:
- `tenant_id` column with FOREIGN KEY to `tenants` table
- Row-Level Security (RLS) policy to enforce isolation
- Automatic tenant context injection via `SecurityUtils`

### 2. Audit Trail Coverage
All Phase F-J entities log changes via:
- `@EntityListeners(AuditLogListener.class)` on entities
- `AuditLog` entries with `createdBy`, `modifiedBy`, `reason`
- Immutable change history queryable via `/api/audit/{entityId}`

### 3. Performance Optimization
- Composite indexes on search columns
- Lazy loading for large relationships
- Query result pagination with `Pageable`
- Cache-aside pattern for read-heavy data

### 4. API Standardization
All Phase F-J REST endpoints follow pattern:
```
GET    /api/{resource}               - List with pagination
GET    /api/{resource}/{id}          - Get single
POST   /api/{resource}               - Create
PUT    /api/{resource}/{id}          - Update
DELETE /api/{resource}/{id}          - Soft delete
GET    /api/{resource}/{id}/history  - Audit trail
```

### 5. Error Handling & Validation
- Input validation via `@Valid` + `@NotNull`, `@Min`, etc.
- Global exception handler via `@ControllerAdvice`
- Structured error responses with error codes
- Logging of validation failures for audit

### 6. Security Across Phases
- All endpoints require `@PreAuthorize("hasAuthority(...)")`
- Sensitive methods audited via `@LogSensitiveAccess`
- Input sanitization against injection attacks
- Rate limiting on public APIs
- CORS restrictions on sensitive endpoints

---

## Database Migration Strategy

**Version Numbering**: V20-V25 for Phases F-I

**Order of Execution**:
1. V20: Operational Analytics (Phase F)
2. V21: Patient Cohort Analysis (Phase F)
3. V22: Security Incidents (Phase H)
4. V23: Backup Audit (Phase I)
5. V24: Recovery Plans (Phase I)
6. V25: Notifications (Phase J)

**Migration Best Practices**:
- Backward compatibility maintained during rollout
- Zero-downtime migrations with separate read/write phases
- Rollback procedures documented for each migration
- Test migrations on staging environment first

---

## ISO 27001 Alignment (Phases F-J)

| Phase | ISO 27001 Controls | Coverage |
|-------|-------------------|----------|
| **F** | A.18 (Compliance) | Regulatory reporting for DPDP Act, Clinical Establishments Act |
| **G** | A.14 (System Development) | Performance testing, load testing, query optimization |
| **H** | A.10 (Cryptography), A.12 (Access Control) | Encryption at rest, threat detection, anomaly detection |
| **I** | A.17 (ISMS Assessment) | Backup verification, recovery testing, disaster recovery drills |
| **J** | A.12.4 (Logging) | Notification audit trail, message retention, read receipts |

---

## Success Metrics by Phase

**Phase F**: Analytics Dashboard
- Daily active users accessing dashboard: >80%
- Report generation time: <30 seconds
- Cohort retention tracking accuracy: >98%

**Phase G**: Performance Optimization
- API response time P95: <200ms (target from <500ms)
- Cache hit rate: >85%
- Database query time: <100ms for 90th percentile

**Phase H**: Security Hardening
- Mean Time to Detect (MTTD) anomalies: <5 minutes
- False positive rate: <10%
- Incident response time: <4 hours
- Zero confirmed security breaches in production

**Phase I**: Disaster Recovery
- RTO (Recovery Time Objective): <30 minutes
- RPO (Recovery Point Objective): <1 hour
- Backup verification success rate: 99.9%
- Annual DR drill completion: 100%

**Phase J**: User Features
- Real-time notification delivery: <100ms latency
- WebSocket connection stability: >99.9% uptime
- Message throughput: >10,000 messages/minute

---

## Recommended Implementation Order

1. **First Quarter**: Phase F (Analytics) + Phase G (Performance)
   - Justification: High business value, enables data-driven decisions
   - Effort: 5-7 weeks combined

2. **Second Quarter**: Phase H (Security Hardening)
   - Justification: Supports compliance requirements, critical for healthcare
   - Effort: 3-4 weeks

3. **Third Quarter**: Phase I (Disaster Recovery)
   - Justification: Operational resilience, RTO/RPO requirements
   - Effort: 2-3 weeks

4. **Fourth Quarter**: Phase J (User Features)
   - Justification: UX improvements, competitive differentiation
   - Effort: 3-4 weeks

---

## Conclusion

Phases F-J build upon Phase E's compliance foundation to create an enterprise-grade clinic management system with advanced analytics, security hardening, disaster recovery, and modern user features. Each phase maintains the mathematical rigor, multi-tenancy isolation, and security standards established in earlier phases while adding specific business value.

The recommended 1-year implementation timeline allows for:
- Proper testing and validation
- Operational stabilization between phases
- Performance optimization iterations
- Security review cycles
- Training and documentation updates

All phases are designed to integrate seamlessly with existing architecture while minimizing disruption to ongoing operations.

