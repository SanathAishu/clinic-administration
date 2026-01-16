# Security Implementation

Comprehensive security documentation for the Clinic Administration multi-tenant system.

## Table of Contents

1. [Security Overview](#security-overview)
2. [Authentication](#authentication)
3. [Authorization](#authorization)
4. [Multi-Tenant Security](#multi-tenant-security)
5. [Data Protection](#data-protection)
6. [Audit Logging](#audit-logging)
7. [DPDP Act 2023 Compliance](#dpdp-act-2023-compliance)
8. [Security Best Practices](#security-best-practices)
9. [Threat Model](#threat-model)
10. [Security Testing](#security-testing)

## Security Overview

The system implements **defense-in-depth security** with multiple layers of protection:

```
┌─────────────────────────────────────────────────────────────────┐
│                     Security Layers                              │
├─────────────────────────────────────────────────────────────────┤
│ 1. Network Security (TLS 1.3, HTTPS only)                       │
│ 2. Authentication (JWT-based, stateless)                        │
│ 3. Authorization (RBAC + Method-level @PreAuthorize)            │
│ 4. Multi-Tenant Isolation (PostgreSQL Row Level Security)       │
│ 5. Input Validation (Bean Validation + Custom Validators)       │
│ 6. SQL Injection Prevention (Parameterized Queries)             │
│ 7. XSS Prevention (Content Security Policy, Output Encoding)    │
│ 8. CSRF Protection (Token-based)                                │
│ 9. Audit Logging (All data access logged, 7-year retention)     │
│ 10. Data Encryption (At rest: TDE, In transit: TLS)             │
└─────────────────────────────────────────────────────────────────┘
```

### Security Principles

1. **Principle of Least Privilege**: Users granted minimum permissions required
2. **Defense in Depth**: Multiple security layers
3. **Fail Secure**: System defaults to deny access
4. **Separation of Duties**: Role-based access control
5. **Audit Everything**: Comprehensive audit trail
6. **Zero Trust**: Verify every request, trust nothing

## Authentication

### JWT-Based Authentication

The system uses **stateless JWT (JSON Web Token) authentication** with refresh token support.

#### Authentication Flow

```
1. Login Request
   POST /api/auth/login
   { "email": "doctor@clinic.com", "password": "***" }
   ↓
2. Credential Validation
   - Check email exists
   - Verify password (BCrypt)
   - Check account status (active, not locked)
   - Validate tenant membership
   ↓
3. JWT Generation
   - Create access token (24-hour expiration)
   - Create refresh token (7-day expiration)
   - Include claims: userId, tenantId, roles, permissions
   ↓
4. Response
   {
     "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
     "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
     "tokenType": "Bearer",
     "expiresIn": 86400
   }
   ↓
5. Subsequent Requests
   Authorization: Bearer <accessToken>
   ↓
6. JWT Validation
   - Verify signature (HMAC-SHA512)
   - Check expiration
   - Extract tenant ID, user ID
   - Set security context
   ↓
7. Refresh Token Flow (when access token expires)
   POST /api/auth/refresh
   { "refreshToken": "..." }
   → New access token issued
```

### JWT Token Structure

#### Access Token Claims

```json
{
  "sub": "123e4567-e89b-12d3-a456-426614174000",  // User ID
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "doctor@clinic.com",
  "roles": ["DOCTOR", "USER"],
  "permissions": ["PATIENT:READ", "PATIENT:CREATE", "APPOINTMENT:READ"],
  "iat": 1705363200,  // Issued at
  "exp": 1705449600   // Expiration (24 hours)
}
```

#### Refresh Token Claims

```json
{
  "sub": "123e4567-e89b-12d3-a456-426614174000",  // User ID
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "refresh",
  "iat": 1705363200,  // Issued at
  "exp": 1705968000   // Expiration (7 days)
}
```

### JWT Configuration

**application.yml**
```yaml
clinic:
  jwt:
    secret: ${JWT_SECRET:changeme-this-is-a-very-long-secret-key-for-jwt-signing-minimum-512-bits}
    expiration: ${JWT_EXPIRATION:86400000}  # 24 hours in milliseconds
    refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}  # 7 days
```

### Password Security

#### Password Hashing: BCrypt

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // 2^12 rounds
}
```

**BCrypt properties:**
- Adaptive hash function (cost factor can be increased over time)
- Built-in salt (unique per password)
- Slow by design (prevents brute-force attacks)
- Cost factor 12 = ~200-300ms per hash (good balance)

#### Password Requirements

Enforced via Bean Validation:

```java
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
    message = "Password must be at least 8 characters with uppercase, lowercase, digit, and special character"
)
private String password;
```

**Requirements:**
- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 digit
- At least 1 special character

### Account Lockout

Protection against brute-force attacks:

```java
clinic:
  security:
    max-login-attempts: 5
    lockout-duration: 900000  # 15 minutes in milliseconds
```

**Lockout logic:**
1. Track failed login attempts per user
2. After 5 failed attempts, lock account for 15 minutes
3. Reset counter on successful login
4. Notify user via email on lockout

### Session Management

Although JWT is stateless, the system maintains session records for:
- Audit trail
- Token revocation (logout)
- Concurrent session limits (future)

```sql
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    access_token_jti VARCHAR(255) NOT NULL,  -- JWT ID
    refresh_token_jti VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    last_activity_at TIMESTAMPTZ
);
```

## Authorization

### Role-Based Access Control (RBAC)

The system implements **fine-grained RBAC** with role-permission mapping.

#### Permission Model

```
User ───(many-to-many)───▶ Role ───(many-to-many)───▶ Permission
  │                          │
  └────── Tenant ────────────┘
```

#### Core Roles

| Role | Description | Typical Permissions |
|------|-------------|---------------------|
| **SUPER_ADMIN** | System administrator | All permissions across all tenants |
| **ADMIN** | Clinic administrator | Tenant management, user management |
| **DOCTOR** | Medical practitioner | Patient care, prescriptions, diagnoses |
| **NURSE** | Nursing staff | Vitals, patient care assistance |
| **RECEPTIONIST** | Front desk staff | Appointments, billing, check-in |
| **LAB_TECH** | Laboratory technician | Lab tests, results entry |
| **PHARMACIST** | Pharmacy staff | Prescription viewing, dispensing |

#### Permission Format

```
{RESOURCE}:{ACTION}
```

**Examples:**
- `PATIENT:CREATE`
- `PATIENT:READ`
- `PATIENT:UPDATE`
- `PATIENT:DELETE`
- `APPOINTMENT:CREATE`
- `PRESCRIPTION:WRITE`
- `LAB_ORDER:CREATE`
- `USER:MANAGE`
- `BILLING:VIEW`

#### Permission Groups

```sql
-- Patient Management
PATIENT:CREATE, PATIENT:READ, PATIENT:UPDATE, PATIENT:DELETE, PATIENT:EXPORT

-- Appointment Management
APPOINTMENT:CREATE, APPOINTMENT:READ, APPOINTMENT:UPDATE, APPOINTMENT:CANCEL

-- Clinical Documentation
ENCOUNTER:CREATE, ENCOUNTER:READ, ENCOUNTER:UPDATE, ENCOUNTER:SIGN

-- Prescriptions
PRESCRIPTION:CREATE, PRESCRIPTION:READ, PRESCRIPTION:UPDATE, PRESCRIPTION:DELETE

-- Lab Orders
LAB_ORDER:CREATE, LAB_ORDER:READ, LAB_ORDER:UPDATE, LAB_ORDER:RESULT_ENTRY

-- User Management
USER:CREATE, USER:READ, USER:UPDATE, USER:DELETE, USER:ASSIGN_ROLE

-- Billing
BILLING:CREATE, BILLING:READ, BILLING:UPDATE, BILLING:PROCESS_PAYMENT

-- Reports & Analytics
REPORT:VIEW, REPORT:EXPORT

-- System Administration
TENANT:MANAGE, BRANCH:MANAGE, AUDIT_LOG:VIEW
```

### Method-Level Security

Using Spring Security `@PreAuthorize` and `@PostAuthorize` annotations:

#### Role-Based Authorization

```java
@Service
public class PatientService {

    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE')")
    public Patient createPatient(CreatePatientRequest request) {
        // Only doctors and nurses can create patients
    }

    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public void checkInPatient(UUID patientId) {
        // Only receptionists and admins can check in patients
    }
}
```

#### Permission-Based Authorization

```java
@Service
public class PrescriptionService {

    @PreAuthorize("hasPermission(#patientId, 'PATIENT', 'READ')")
    public List<Prescription> getPatientPrescriptions(UUID patientId) {
        // Check if user has PATIENT:READ permission
    }

    @PreAuthorize("hasAuthority('PRESCRIPTION:CREATE')")
    public Prescription createPrescription(CreatePrescriptionRequest request) {
        // Check if user has PRESCRIPTION:CREATE authority
    }
}
```

#### Expression-Based Authorization

```java
@Service
public class BillingService {

    @PreAuthorize("@securityService.canAccessPatientData(#patientId)")
    public Billing createBilling(UUID patientId, CreateBillingRequest request) {
        // Custom security expression
    }

    @PostAuthorize("returnObject.tenantId == authentication.principal.tenantId")
    public Billing getBillingById(UUID id) {
        // Post-authorization check: verify tenant ownership
    }
}
```

#### Custom Security Expressions

```java
@Component("securityService")
public class SecurityService {

    public boolean canAccessPatientData(UUID patientId) {
        UUID currentTenantId = TenantContext.getCurrentTenant();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        // Check if patient belongs to current tenant
        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient == null || !patient.getTenantId().equals(currentTenantId)) {
            return false;
        }

        // Additional business logic checks
        return true;
    }

    public boolean canModifyUser(UUID userId) {
        // Can only modify users in same tenant
        // Admins can modify any user in their tenant
        // Users can only modify their own profile
    }
}
```

### Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enables @PreAuthorize, @PostAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)  // Disabled for stateless JWT
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/api/auth/**",           // Login, register, refresh
                    "/api/public/**",         // Public resources
                    "/actuator/health",       // Health check
                    "/actuator/prometheus",   // Metrics
                    "/api/docs/**",           // API documentation
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()

                // OPTIONS requests (CORS pre-flight)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

## Multi-Tenant Security

### Row Level Security (RLS)

**Database-level tenant isolation** using PostgreSQL Row Level Security.

#### How RLS Works

```sql
-- 1. Enable RLS on tenant-aware tables
ALTER TABLE patients ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE billing ENABLE ROW LEVEL SECURITY;

-- 2. Create RLS policy
CREATE POLICY tenant_isolation ON patients
    USING (tenant_id::text = current_setting('app.tenant_id', TRUE));

-- 3. Application sets session variable
SET app.tenant_id = '550e8400-e29b-41d4-a716-446655440000';

-- 4. All queries automatically filtered
SELECT * FROM patients;  -- Only returns current tenant's patients
INSERT INTO patients (...) VALUES (...);  -- tenant_id automatically validated
```

#### Tenant Context Management

```java
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final DataSource dataSource;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extract tenant ID from JWT
            String token = jwtTokenProvider.resolveToken(request);
            if (token != null && jwtTokenProvider.validateToken(token)) {
                UUID tenantId = jwtTokenProvider.getTenantId(token);

                // Set ThreadLocal tenant context
                TenantContext.setCurrentTenant(tenantId);

                // Set PostgreSQL session variable
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("SET app.tenant_id = '" + tenantId + "'");
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clear context
            TenantContext.clear();
        }
    }
}
```

### Benefits of RLS

1. **Defense in Depth**: Database enforces isolation even if application has bugs
2. **No Application Filtering**: Queries automatically filtered, no manual WHERE clauses
3. **Audit Trail**: Database logs all tenant access
4. **Performance**: Database optimizes RLS policy queries
5. **Regulatory Compliance**: Strong data isolation for DPDP Act compliance

### Cross-Tenant Access Prevention

```java
@PrePersist
@PreUpdate
void validateTenant() {
    UUID contextTenantId = TenantContext.getCurrentTenant();
    if (this.tenantId != null && !this.tenantId.equals(contextTenantId)) {
        throw new SecurityException(
            "Tenant mismatch: attempting to modify entity of different tenant"
        );
    }
}
```

## Data Protection

### Encryption at Rest

**Database-level encryption:**
- PostgreSQL Transparent Data Encryption (TDE)
- Encrypted database volumes
- Encrypted backups

**Application-level sensitive field encryption (future):**
```java
@Convert(converter = EncryptedStringConverter.class)
private String socialSecurityNumber;

@Convert(converter = EncryptedStringConverter.class)
private String bankAccountNumber;
```

### Encryption in Transit

**TLS 1.3 for all communications:**
- Client ↔ Backend: HTTPS only
- Backend ↔ Database: SSL/TLS
- Backend ↔ Redis: SSL/TLS (production)
- Backend ↔ RabbitMQ: SSL/TLS (production)
- Backend ↔ MinIO: HTTPS (production)

**CORS Configuration:**
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowCredentials(true);
    configuration.setAllowedOriginPatterns(List.of(
        "https://clinic.example.com",
        "https://*.clinic.example.com"
    ));
    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    ));
    configuration.setAllowedHeaders(Arrays.asList(
        "Authorization", "Content-Type", "X-Tenant-ID", "X-Request-ID"
    ));
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### Input Validation

**Bean Validation (JSR-380):**

```java
@Data
public class CreatePatientRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be 2-50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "First name must contain only letters")
    private String firstName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    @Pattern(regexp = "^[0-9]{14}$", message = "ABHA ID must be 14 digits")
    private String abhaId;
}
```

**SQL Injection Prevention:**
- Always use parameterized queries (JPA, JDBC PreparedStatement)
- Never concatenate user input into SQL
- Use JPA Criteria API or QueryDSL for dynamic queries

```java
// GOOD: Parameterized query
@Query("SELECT u FROM User u WHERE u.email = :email AND u.tenantId = :tenantId")
Optional<User> findByEmailAndTenantId(@Param("email") String email,
                                       @Param("tenantId") UUID tenantId);

// GOOD: JPA repository method
Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

// BAD: String concatenation (vulnerable to SQL injection)
// String sql = "SELECT * FROM users WHERE email = '" + email + "'";  // NEVER DO THIS
```

### XSS Prevention

**Content Security Policy (CSP):**
```java
@Bean
public FilterRegistrationBean<CSPFilter> cspFilter() {
    FilterRegistrationBean<CSPFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new CSPFilter());
    registrationBean.addUrlPatterns("/*");
    return registrationBean;
}

public class CSPFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader("Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self'; " +
            "frame-ancestors 'none';"
        );
        chain.doFilter(request, response);
    }
}
```

**Output Encoding (Frontend):**
- Use React's built-in XSS protection (JSX auto-escapes)
- Sanitize HTML content with DOMPurify
- Avoid `dangerouslySetInnerHTML` unless absolutely necessary

### CSRF Protection

**Token-based CSRF protection:**

While the system uses stateless JWT authentication (no cookies), CSRF tokens are still used for:
- Form submissions (future frontend forms)
- State-changing operations

```java
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
);
```

## Audit Logging

### Comprehensive Audit Trail

**All data access and modifications are logged** for compliance and security monitoring.

#### Audit Log Schema

```sql
CREATE TABLE audit_logs (
    id UUID NOT NULL DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    action audit_action NOT NULL,  -- CREATE, READ, UPDATE, DELETE, EXPORT, PRINT
    entity_type VARCHAR(50) NOT NULL,  -- 'patients', 'appointments', etc.
    entity_id UUID NOT NULL,
    performed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    before_state JSONB,  -- State before modification
    after_state JSONB,   -- State after modification
    PRIMARY KEY (id, performed_at)
) PARTITION BY RANGE (performed_at);

-- Monthly partitions for performance
CREATE TABLE audit_logs_2026_01 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
```

#### Audit Actions

```java
public enum AuditAction {
    CREATE,    // Insert new record
    READ,      // View record (sensitive data)
    UPDATE,    // Modify existing record
    DELETE,    // Soft delete record
    EXPORT,    // Export data (CSV, PDF)
    PRINT      // Print document
}
```

#### Automatic Audit Logging

Using AOP (Aspect-Oriented Programming):

```java
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final HttpServletRequest request;

    @Around("@annotation(com.clinic.common.annotation.Audited)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Object beforeState = args.length > 0 ? args[0] : null;

        Object result = joinPoint.proceed();  // Execute method

        Object afterState = result;

        // Create audit log entry
        AuditLog auditLog = AuditLog.builder()
            .tenantId(TenantContext.getCurrentTenant())
            .userId(SecurityUtils.getCurrentUserId())
            .action(extractAction(joinPoint))
            .entityType(extractEntityType(joinPoint))
            .entityId(extractEntityId(result))
            .ipAddress(request.getRemoteAddr())
            .userAgent(request.getHeader("User-Agent"))
            .beforeState(toJson(beforeState))
            .afterState(toJson(afterState))
            .build();

        auditLogRepository.save(auditLog);

        return result;
    }
}
```

#### Usage Example

```java
@Service
public class PatientService {

    @Audited(action = AuditAction.CREATE, entityType = "patients")
    @Transactional
    public Patient createPatient(CreatePatientRequest request) {
        // Create patient logic
    }

    @Audited(action = AuditAction.UPDATE, entityType = "patients")
    @Transactional
    public Patient updatePatient(UUID id, UpdatePatientRequest request) {
        // Update patient logic
    }

    @Audited(action = AuditAction.DELETE, entityType = "patients")
    @Transactional
    public void softDeletePatient(UUID id) {
        // Soft delete logic
    }

    @Audited(action = AuditAction.EXPORT, entityType = "patients")
    public byte[] exportPatientData(UUID id) {
        // Export logic
    }
}
```

### Audit Log Retention

**7-year retention** as mandated by Clinical Establishments Act:

```yaml
clinic:
  audit:
    enabled: true
    retention-days: 2555  # 7 years
```

### Audit Log Queries

```sql
-- All patient access by a specific user
SELECT * FROM audit_logs
WHERE user_id = '123e4567-e89b-12d3-a456-426614174000'
  AND entity_type = 'patients'
ORDER BY performed_at DESC;

-- All modifications to a specific patient
SELECT * FROM audit_logs
WHERE entity_id = '456e7890-e12b-34c5-d678-901234567890'
  AND entity_type = 'patients'
  AND action IN ('CREATE', 'UPDATE', 'DELETE')
ORDER BY performed_at DESC;

-- All exports in the last 30 days
SELECT * FROM audit_logs
WHERE action = 'EXPORT'
  AND performed_at >= CURRENT_TIMESTAMP - INTERVAL '30 days'
ORDER BY performed_at DESC;

-- Suspicious access patterns (multiple failed attempts)
SELECT user_id, COUNT(*) as failed_attempts
FROM audit_logs
WHERE action = 'LOGIN_FAILED'
  AND performed_at >= CURRENT_TIMESTAMP - INTERVAL '1 hour'
GROUP BY user_id
HAVING COUNT(*) >= 5;
```

## DPDP Act 2023 Compliance

### Digital Personal Data Protection Act 2023

The system implements key requirements of India's data protection law.

#### Consent Management

```sql
CREATE TABLE patient_consents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    patient_id UUID NOT NULL REFERENCES patients(id),
    consent_type consent_type NOT NULL,  -- TREATMENT, DATA_PROCESSING, COMMUNICATION
    status consent_status NOT NULL,       -- PENDING, GIVEN, WITHDRAWN
    purpose TEXT NOT NULL,
    given_at TIMESTAMPTZ,
    given_by UUID REFERENCES users(id),
    withdrawn_at TIMESTAMPTZ,
    withdrawn_by UUID REFERENCES users(id),
    expiry_date TIMESTAMPTZ,
    consent_text TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### Consent Types

| Type | Purpose | Required For |
|------|---------|--------------|
| **TREATMENT** | Medical treatment and procedures | All clinical operations |
| **DATA_PROCESSING** | Processing personal health data | Data storage, analysis |
| **COMMUNICATION** | Marketing, reminders, notifications | Non-essential communications |

#### Right to Erasure

**Soft delete only** to maintain audit trail while respecting right to erasure:

```java
@Service
public class PatientService {

    @Transactional
    public void exerciseRightToErasure(UUID patientId, UUID tenantId) {
        Patient patient = getPatientById(patientId, tenantId);

        // Soft delete patient
        patient.setDeletedAt(Instant.now());
        patient.setDeletedBy(SecurityUtils.getCurrentUserId());

        // Anonymize sensitive data (GDPR-style pseudonymization)
        patient.setFirstName("REDACTED");
        patient.setLastName("REDACTED");
        patient.setEmail("redacted-" + patient.getId() + "@deleted.local");
        patient.setPhone(null);
        patient.setAddress(null);
        patient.setAbhaId(null);

        // Keep medical data for legal retention period
        // Soft delete does not remove data from database

        patientRepository.save(patient);

        // Audit the erasure request
        auditLogRepository.save(AuditLog.builder()
            .action(AuditAction.ERASURE_REQUEST)
            .entityType("patients")
            .entityId(patientId)
            .build());
    }
}
```

#### Data Breach Notification

**72-hour notification window:**

```java
@Service
public class DataBreachService {

    public void reportDataBreach(DataBreachIncident incident) {
        // Log breach incident
        breachLogRepository.save(incident);

        // Notify data protection officer
        notificationService.notifyDPO(incident);

        // Notify affected individuals (if high risk)
        if (incident.getSeverity() == Severity.HIGH) {
            notificationService.notifyAffectedUsers(incident);
        }

        // Notify regulatory authority (within 72 hours)
        if (shouldNotifyAuthority(incident)) {
            authorityNotificationService.notifyAuthority(incident);
        }
    }
}
```

### IT Act 2000 & SPDI Rules

**Reasonable Security Practices:**
1. Encryption at rest and in transit
2. Role-based access control
3. Audit logging
4. Regular security assessments
5. Incident response plan

## Security Best Practices

### Development Best Practices

1. **Never Log Sensitive Data**
   ```java
   // BAD
   log.info("User password: {}", password);

   // GOOD
   log.info("User login attempt: {}", email);
   ```

2. **Use Parameterized Queries**
   ```java
   // BAD
   String sql = "SELECT * FROM users WHERE email = '" + email + "'";

   // GOOD
   @Query("SELECT u FROM User u WHERE u.email = :email")
   Optional<User> findByEmail(@Param("email") String email);
   ```

3. **Validate All Input**
   ```java
   @PostMapping("/patients")
   public ResponseEntity<PatientDTO> createPatient(
       @Valid @RequestBody CreatePatientRequest request) {
       // @Valid triggers Bean Validation
   }
   ```

4. **Use HTTPS Everywhere**
   ```yaml
   server:
     ssl:
       enabled: true
       key-store: classpath:keystore.p12
       key-store-password: ${SSL_KEYSTORE_PASSWORD}
   ```

5. **Implement Rate Limiting**
   ```java
   @RateLimiter(name = "login", fallbackMethod = "loginRateLimitFallback")
   public AuthResponse login(LoginRequest request) {
       // Login logic
   }
   ```

### Deployment Best Practices

1. **Use Environment Variables for Secrets**
   ```yaml
   spring:
     datasource:
       password: ${DB_PASSWORD}  # Never hardcode
   ```

2. **Enable Security Headers**
   ```java
   http.headers(headers -> headers
       .contentSecurityPolicy("default-src 'self'")
       .frameOptions(frameOptions -> frameOptions.deny())
       .xssProtection()
       .httpStrictTransportSecurity()
   );
   ```

3. **Regular Security Updates**
   ```bash
   # Update dependencies regularly
   ./gradlew dependencyUpdates
   ```

4. **Monitor Security Metrics**
   - Failed login attempts
   - Unauthorized access attempts
   - Unusual data access patterns
   - Audit log anomalies

## Threat Model

### Identified Threats

| Threat | Likelihood | Impact | Mitigation |
|--------|-----------|--------|------------|
| SQL Injection | Low | Critical | Parameterized queries, input validation |
| XSS | Medium | High | CSP, output encoding, React auto-escape |
| CSRF | Low | Medium | CSRF tokens, SameSite cookies |
| Brute Force | Medium | High | Rate limiting, account lockout, CAPTCHA |
| Session Hijacking | Low | Critical | HTTPS only, secure tokens, short expiry |
| Privilege Escalation | Low | Critical | RBAC, method-level security, RLS |
| Data Breach | Low | Critical | Encryption, access controls, audit logs |
| DDoS | Medium | Medium | Rate limiting, CDN, load balancing |

### Security Controls Matrix

| Control | Type | Status |
|---------|------|--------|
| JWT Authentication | Preventive | ✅ Implemented |
| RBAC Authorization | Preventive | ✅ Implemented |
| Row Level Security | Preventive | ✅ Implemented |
| Input Validation | Preventive | ✅ Implemented |
| Output Encoding | Preventive | ⚠️ Partial (Frontend) |
| Audit Logging | Detective | ✅ Implemented |
| Encryption (TLS) | Preventive | ✅ Implemented |
| Encryption (TDE) | Preventive | ⚠️ Production only |
| Rate Limiting | Preventive | 📋 Planned |
| CAPTCHA | Preventive | 📋 Planned |
| WAF | Preventive | 📋 Planned |
| SIEM Integration | Detective | 📋 Planned |

## Security Testing

### Security Test Checklist

- [ ] Authentication bypass attempts
- [ ] Authorization bypass (horizontal/vertical privilege escalation)
- [ ] SQL injection testing (SQLMap)
- [ ] XSS testing (manual + automated)
- [ ] CSRF token validation
- [ ] Session management testing
- [ ] Input validation bypass attempts
- [ ] Rate limiting validation
- [ ] Multi-tenant isolation verification
- [ ] Audit log integrity verification

### Automated Security Scanning

**OWASP Dependency Check:**
```bash
./gradlew dependencyCheckAnalyze
```

**SonarQube Security Analysis:**
```bash
./gradlew sonarqube
```

**SAST (Static Application Security Testing):**
- SpotBugs with security rules
- PMD security ruleset
- CheckStyle security checks

### Penetration Testing

**Recommended tools:**
- OWASP ZAP (automated scanning)
- Burp Suite Professional (manual testing)
- SQLMap (SQL injection testing)
- Postman (API security testing)

## Incident Response

### Security Incident Response Plan

1. **Detection**: Monitor logs, alerts, anomaly detection
2. **Analysis**: Determine scope, severity, affected systems
3. **Containment**: Isolate affected systems, revoke compromised credentials
4. **Eradication**: Remove threat, patch vulnerabilities
5. **Recovery**: Restore systems, verify integrity
6. **Post-Incident**: Document lessons learned, update security controls

### Contact Information

- **Security Team**: security@clinic.example.com
- **Data Protection Officer**: dpo@clinic.example.com
- **Incident Hotline**: +91-XXX-XXX-XXXX

## References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/index.html)
- [DPDP Act 2023](https://www.meity.gov.in/writereaddata/files/Digital%20Personal%20Data%20Protection%20Act%202023.pdf)
- [IT Act 2000](https://www.meity.gov.in/content/information-technology-act-2000)
- [ABDM Security Guidelines](https://abdm.gov.in/)
- [ARCHITECTURE.md](ARCHITECTURE.md)
- [CACHING.md](CACHING.md)

---

**Document Version**: 1.0
**Last Updated**: 2026-01-16
**Maintained By**: Security Team
