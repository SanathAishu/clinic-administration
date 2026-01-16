# Caching Strategy

Comprehensive documentation for the Redis-based distributed caching implementation in the Clinic Administration system.

## Table of Contents

1. [Overview](#overview)
2. [Cache Architecture](#cache-architecture)
3. [Cache Regions](#cache-regions)
4. [Cache Key Strategy](#cache-key-strategy)
5. [TTL Configuration](#ttl-configuration)
6. [Cache Invalidation](#cache-invalidation)
7. [Multi-Tenancy Support](#multi-tenancy-support)
8. [Usage Examples](#usage-examples)
9. [Monitoring and Metrics](#monitoring-and-metrics)
10. [Best Practices](#best-practices)
11. [Troubleshooting](#troubleshooting)

## Overview

The system uses **Redis 7** as a distributed cache layer to improve performance and reduce database load.

### Key Features

- **Distributed Caching**: Redis cluster-ready for horizontal scaling
- **Multi-Tenant Aware**: Automatic tenant ID prefixing for data isolation
- **TTL-Based Eviction**: Configurable time-to-live per cache region
- **JSON Serialization**: Supports complex Java objects including Java 8 date/time
- **Cache Statistics**: Monitoring via Spring Boot Actuator
- **Type-Safe Configuration**: Compile-time cache name constants

### Why Redis?

| Feature | Benefit |
|---------|---------|
| **In-Memory** | Sub-millisecond latency for reads |
| **Distributed** | Shared cache across multiple application instances |
| **Persistence** | AOF (Append-Only File) for data durability |
| **Data Structures** | Supports strings, lists, sets, hashes, sorted sets |
| **TTL Support** | Automatic key expiration |
| **Pub/Sub** | Real-time messaging (future use) |
| **Cluster Mode** | Horizontal scalability |

## Cache Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                      Application Layer                            │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Service Layer                                              │  │
│  │ @Cacheable, @CacheEvict, @CachePut annotations            │  │
│  └────────────────┬───────────────────────────────────────────┘  │
│                   │                                               │
│  ┌────────────────▼───────────────────────────────────────────┐  │
│  │ Spring Cache Abstraction                                   │  │
│  │ - CacheManager                                             │  │
│  │ - TenantAwareCacheKeyGenerator                            │  │
│  └────────────────┬───────────────────────────────────────────┘  │
└───────────────────┼───────────────────────────────────────────────┘
                    │
┌───────────────────▼───────────────────────────────────────────────┐
│                   Redis Cache Layer                               │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ Cache Regions (7 regions with different TTLs)              │ │
│  │                                                             │ │
│  │ roles          │ users         │ patients                  │ │
│  │ TTL: 10 min    │ TTL: 10 min   │ TTL: 5 min               │ │
│  │                │               │                           │ │
│  │ appointments   │ appointments: │ billings                  │ │
│  │ TTL: 2 min     │ today         │ TTL: 3 min               │ │
│  │                │ TTL: 1 min    │                           │ │
│  │                │               │                           │ │
│  │ billings:summary (TTL: 10 min)                             │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  Key Format: {tenantId}:{cacheName}:{methodParams}               │
│  Example: "550e8400-e29b-41d4-a716-446655440000:users:list"      │
│                                                                   │
│  Serialization: GenericJackson2JsonRedisSerializer                │
│  - JSON format for human readability                              │
│  - Type information preserved                                     │
│  - Java 8 date/time support                                       │
└───────────────────────────────────────────────────────────────────┘
```

## Cache Regions

The system defines **7 cache regions** optimized for different data volatility patterns.

### Complete Cache Region Specification

| Cache Name | TTL | Volatility | Data Type | Use Case |
|------------|-----|------------|-----------|----------|
| `roles` | 10 min | Low | User roles & permissions | Authorization checks, role listings |
| `users` | 10 min | Low | User entities, profiles | User lookups, profile data |
| `patients` | 5 min | Medium | Patient demographics | Patient search, profile views |
| `appointments` | 2 min | High | Appointment schedules | Appointment listings, calendar |
| `appointments:today` | 1 min | Very High | Today's appointments | Today's schedule dashboard |
| `billings` | 3 min | Medium | Billing records | Invoice listings, payment status |
| `billings:summary` | 10 min | Low | Historical summaries | Financial reports, analytics |

### Cache Region Design Rationale

#### Low Volatility (10-minute TTL)

**Roles & Permissions**
- Changes infrequently (admin operation)
- Critical for authorization checks
- High read-to-write ratio (1000:1)
- Safe to cache longer

**Billing Summaries**
- Historical data (read-only)
- Computed aggregations (expensive)
- No real-time requirements
- Perfect for long caching

**Users**
- Profile changes are rare
- Authentication needs fast lookups
- High concurrent reads
- 10-minute staleness acceptable

#### Medium Volatility (3-5 minute TTL)

**Patients**
- Demographics change occasionally
- Medical data updates moderately
- Balance between freshness and performance
- 5-minute staleness tolerable

**Billings**
- Payment status updates frequently
- New invoices added throughout day
- 3-minute TTL for reasonable freshness
- Evicted on payment processing

#### High Volatility (1-2 minute TTL)

**Appointments**
- Constantly changing (bookings, cancellations)
- High user interaction
- 2-minute TTL minimizes stale data
- Evicted on any appointment modification

**Today's Appointments**
- Real-time critical
- Dashboard views require freshness
- 1-minute TTL for near-real-time updates
- Separate cache for optimization

## Cache Key Strategy

### Key Generation

The system uses a **tenant-aware cache key generator** to ensure multi-tenant data isolation.

#### Key Format

```
{tenantId}:{cacheName}:{methodName}:{params}
```

#### Examples

```
# User list for tenant 550e8400-e29b-41d4-a716-446655440000
550e8400-e29b-41d4-a716-446655440000:users:list

# User detail for specific user ID
550e8400-e29b-41d4-a716-446655440000:users:detail:123e4567-e89b-12d3-a456-426614174000

# Doctor list for tenant
550e8400-e29b-41d4-a716-446655440000:users:doctors

# Search results
550e8400-e29b-41d4-a716-446655440000:users:search:john

# Patient appointments
550e8400-e29b-41d4-a716-446655440000:patients:appointments:123e4567-e89b-12d3-a456-426614174000

# Today's appointments
550e8400-e29b-41d4-a716-446655440000:appointments:today:2026-01-16

# Billing for current month
550e8400-e29b-41d4-a716-446655440000:billings:currentMonth:2026-01
```

### TenantAwareCacheKeyGenerator Implementation

```java
@Component
public class TenantAwareCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        UUID tenantId = TenantContext.getCurrentTenant();

        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not set");
        }

        String cacheName = method.getName();
        String paramsKey = Arrays.stream(params)
            .map(param -> param != null ? param.toString() : "null")
            .collect(Collectors.joining(":"));

        return String.format("%s:%s:%s", tenantId, cacheName, paramsKey);
    }
}
```

### Custom Key Generation

For more control, use explicit `key` attribute:

```java
@Cacheable(value = "users", key = "#tenantId + ':list'")
public List<UserListViewDTO> getUserListView(UUID tenantId) {
    // Implementation
}

@Cacheable(value = "users", key = "#tenantId + ':detail:' + #id")
public Optional<UserDetailViewDTO> getUserDetailView(UUID id, UUID tenantId) {
    // Implementation
}

@Cacheable(value = "appointments", key = "#tenantId + ':today:' + T(java.time.LocalDate).now()")
public List<AppointmentListViewDTO> getTodayAppointments(UUID tenantId) {
    // Implementation
}
```

## TTL Configuration

### Configuration Class

```java
@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    // Cache name constants
    public static final String ROLES_CACHE = "roles";
    public static final String USERS_CACHE = "users";
    public static final String PATIENTS_CACHE = "patients";
    public static final String APPOINTMENTS_CACHE = "appointments";
    public static final String APPOINTMENTS_TODAY_CACHE = "appointments:today";
    public static final String BILLINGS_CACHE = "billings";
    public static final String BILLINGS_SUMMARY_CACHE = "billings:summary";

    // TTL configuration
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    private static final Duration ROLES_TTL = Duration.ofMinutes(10);
    private static final Duration USERS_TTL = Duration.ofMinutes(10);
    private static final Duration PATIENTS_TTL = Duration.ofMinutes(5);
    private static final Duration APPOINTMENTS_TTL = Duration.ofMinutes(2);
    private static final Duration APPOINTMENTS_TODAY_TTL = Duration.ofMinutes(1);
    private static final Duration BILLINGS_TTL = Duration.ofMinutes(3);
    private static final Duration BILLINGS_SUMMARY_TTL = Duration.ofMinutes(10);

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(createJsonSerializer()))
                .disableCachingNullValues();

        // Per-cache TTL configuration
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(ROLES_CACHE, defaultConfig.entryTtl(ROLES_TTL));
        cacheConfigurations.put(USERS_CACHE, defaultConfig.entryTtl(USERS_TTL));
        cacheConfigurations.put(PATIENTS_CACHE, defaultConfig.entryTtl(PATIENTS_TTL));
        cacheConfigurations.put(APPOINTMENTS_CACHE, defaultConfig.entryTtl(APPOINTMENTS_TTL));
        cacheConfigurations.put(APPOINTMENTS_TODAY_CACHE,
            defaultConfig.entryTtl(APPOINTMENTS_TODAY_TTL));
        cacheConfigurations.put(BILLINGS_CACHE, defaultConfig.entryTtl(BILLINGS_TTL));
        cacheConfigurations.put(BILLINGS_SUMMARY_CACHE,
            defaultConfig.entryTtl(BILLINGS_SUMMARY_TTL));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .enableStatistics()
                .build();
    }

    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
```

### Adjusting TTL Values

To adjust TTL for a specific environment:

**application-prod.yml**
```yaml
# Override in environment-specific config
clinic:
  cache:
    roles-ttl: PT15M      # 15 minutes
    users-ttl: PT15M      # 15 minutes
    patients-ttl: PT3M    # 3 minutes
    appointments-ttl: PT1M # 1 minute
```

## Cache Invalidation

### Invalidation Strategies

#### 1. Write-Through Cache (Evict on Write)

Automatically clear cache when data is modified.

```java
@Service
public class UserService {

    // Evict entire user cache on create
    @CacheEvict(value = "users", allEntries = true)
    public User createUser(User user, UUID tenantId) {
        // Create user logic
    }

    // Evict entire user cache on update
    @CacheEvict(value = "users", allEntries = true)
    public User updateUser(UUID id, UUID tenantId, User updates) {
        // Update user logic
    }

    // Evict entire user cache on status change
    @CacheEvict(value = "users", allEntries = true)
    public User updateUserStatus(UUID userId, UUID tenantId, UserStatus newStatus) {
        // Status change logic
    }

    // Evict entire user cache on soft delete
    @CacheEvict(value = "users", allEntries = true)
    public void softDeleteUser(UUID userId, UUID tenantId) {
        // Soft delete logic
    }
}
```

#### 2. Selective Cache Eviction

Evict specific cache entries instead of entire cache.

```java
// Evict specific user detail
@CacheEvict(value = "users", key = "#tenantId + ':detail:' + #userId")
public void updateUserProfile(UUID userId, UUID tenantId, ProfileUpdate update) {
    // Update logic
}

// Evict multiple cache entries
@Caching(evict = {
    @CacheEvict(value = "users", key = "#tenantId + ':detail:' + #userId"),
    @CacheEvict(value = "users", key = "#tenantId + ':list'")
})
public void updateUserRoles(UUID userId, UUID tenantId, Set<UUID> roleIds) {
    // Update logic
}
```

#### 3. Cache Put (Update Cache)

Update cache without evicting.

```java
@CachePut(value = "users", key = "#tenantId + ':detail:' + #result.id")
public UserDetailViewDTO refreshUserDetail(UUID userId, UUID tenantId) {
    // Fetch fresh data and update cache
    return userViewRepository.findDetailById(userId, tenantId).orElseThrow();
}
```

#### 4. Conditional Eviction

Evict cache based on conditions.

```java
// Only evict if tenant context is set
@CacheEvict(value = "billings:detail", allEntries = true,
            condition = "#tenantId != null")
public Billing createBilling(CreateBillingRequest request, UUID tenantId) {
    // Create billing logic
}

// Evict unless result is empty
@Cacheable(value = "users", key = "#tenantId + ':search:' + #searchTerm",
           unless = "#result.isEmpty()")
public List<UserListViewDTO> searchUsers(UUID tenantId, String searchTerm) {
    // Search logic
}
```

### Cache Invalidation Patterns by Entity

| Entity | Invalidation Strategy | Cache Regions Affected |
|--------|----------------------|------------------------|
| **User** | Evict all `users` cache | `users` |
| **Patient** | Evict all `patients` cache | `patients` |
| **Appointment** | Evict `appointments` and `appointments:today` | `appointments`, `appointments:today` |
| **Billing** | Evict `billings` and `billings:summary` | `billings`, `billings:summary` |
| **Role** | Evict all `roles` cache | `roles` |

### Manual Cache Eviction

For administrative operations:

```java
@Service
@RequiredArgsConstructor
public class CacheManagementService {

    private final CacheManager cacheManager;

    public void evictAllCaches() {
        cacheManager.getCacheNames()
            .forEach(cacheName ->
                Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
    }

    public void evictCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    public void evictCacheKey(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}
```

## Multi-Tenancy Support

### Tenant Isolation

Every cache key includes the tenant ID to ensure complete data isolation between tenants.

#### How It Works

```
1. Request arrives with JWT
   ↓
2. JwtAuthenticationFilter extracts tenant ID
   ↓
3. TenantContext.setCurrentTenant(tenantId)
   ↓
4. Service method annotated with @Cacheable
   ↓
5. TenantAwareCacheKeyGenerator generates key
   Key: "{tenantId}:{cacheName}:{params}"
   ↓
6. Redis stores/retrieves data with tenant-prefixed key
   ↓
7. Response returned
   ↓
8. TenantContext.clear() in filter cleanup
```

### Benefits

1. **Security**: No cross-tenant data leakage
2. **Performance**: Each tenant has isolated cache namespace
3. **Scalability**: Tenants can be migrated to different Redis instances
4. **Debugging**: Easy to identify which tenant's data is cached

### Cache Verification

Check tenant isolation:

```bash
# Connect to Redis
docker exec -it clinic-redis redis-cli

# List all keys for a specific tenant
KEYS 550e8400-e29b-41d4-a716-446655440000:*

# Check specific cache entry
GET "550e8400-e29b-41d4-a716-446655440000:users:list"

# Count keys per tenant
KEYS 550e8400-e29b-41d4-a716-446655440000:* | wc -l
```

## Usage Examples

### Example 1: Cacheable Read Operation

```java
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientViewRepository patientViewRepository;

    /**
     * Get patient list - Cached for 5 minutes
     */
    @Cacheable(value = "patients", key = "#tenantId + ':list'")
    public List<PatientListViewDTO> getPatientList(UUID tenantId) {
        return patientViewRepository.findAllByTenantId(tenantId);
    }

    /**
     * Get patient detail - Cached for 5 minutes
     */
    @Cacheable(value = "patients", key = "#tenantId + ':detail:' + #id")
    public Optional<PatientDetailViewDTO> getPatientDetail(UUID id, UUID tenantId) {
        return patientViewRepository.findDetailById(id, tenantId);
    }

    /**
     * Search patients - Cached unless empty result
     */
    @Cacheable(value = "patients",
               key = "#tenantId + ':search:' + #searchTerm",
               unless = "#result.isEmpty()")
    public List<PatientListViewDTO> searchPatients(UUID tenantId, String searchTerm) {
        return patientViewRepository.searchByTenantId(tenantId, searchTerm);
    }
}
```

### Example 2: Cache Eviction on Write

```java
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    /**
     * Create patient - Evict entire patients cache
     */
    @CacheEvict(value = "patients", allEntries = true)
    @Transactional
    public Patient createPatient(CreatePatientRequest request, UUID tenantId) {
        Patient patient = patientMapper.toEntity(request);
        patient.setTenantId(tenantId);
        return patientRepository.save(patient);
    }

    /**
     * Update patient - Evict entire patients cache
     */
    @CacheEvict(value = "patients", allEntries = true)
    @Transactional
    public Patient updatePatient(UUID id, UUID tenantId, UpdatePatientRequest request) {
        Patient patient = getPatientById(id, tenantId);
        patientMapper.updateEntity(patient, request);
        return patientRepository.save(patient);
    }
}
```

### Example 3: Multiple Cache Evictions

```java
@Service
@RequiredArgsConstructor
public class BillingService {

    /**
     * Create billing - Evict multiple caches
     */
    @Caching(evict = {
        @CacheEvict(value = "billings:detail", allEntries = true,
                    condition = "#tenantId != null"),
        @CacheEvict(value = "billings:currentMonth", allEntries = true,
                    condition = "#tenantId != null"),
        @CacheEvict(value = "billings:summary", allEntries = true,
                    condition = "#tenantId != null")
    })
    @Transactional
    public Billing createBilling(CreateBillingRequest request, UUID tenantId) {
        // Create billing logic
    }
}
```

### Example 4: Conditional Caching

```java
@Service
public class AppointmentService {

    /**
     * Get today's appointments - Very short TTL (1 minute)
     */
    @Cacheable(value = "appointments:today",
               key = "#tenantId + ':today:' + T(java.time.LocalDate).now()")
    public List<AppointmentListViewDTO> getTodayAppointments(UUID tenantId) {
        LocalDate today = LocalDate.now();
        return appointmentViewRepository.findTodayAppointments(tenantId, today);
    }

    /**
     * Get upcoming appointments - Cached unless empty
     */
    @Cacheable(value = "appointments",
               key = "#tenantId + ':upcoming'",
               unless = "#result.isEmpty()")
    public List<AppointmentListViewDTO> getUpcomingAppointments(UUID tenantId) {
        return appointmentViewRepository.findUpcomingByTenantId(tenantId);
    }
}
```

## Monitoring and Metrics

### Cache Statistics

Cache statistics are enabled via Spring Boot Actuator.

#### Access Metrics

```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics endpoint
curl http://localhost:8080/actuator/metrics

# Cache-specific metrics
curl http://localhost:8080/actuator/metrics/cache.gets
curl http://localhost:8080/actuator/metrics/cache.puts
curl http://localhost:8080/actuator/metrics/cache.evictions
curl http://localhost:8080/actuator/metrics/cache.hits
curl http://localhost:8080/actuator/metrics/cache.misses
```

#### Prometheus Metrics

```
# Cache hit rate
cache_gets_total{name="users",result="hit"} 1523
cache_gets_total{name="users",result="miss"} 127

# Cache hit ratio
(cache_gets_total{result="hit"} / cache_gets_total) * 100

# Cache evictions
cache_evictions_total{name="users"} 42

# Cache size
cache_size{name="users"} 156
```

### Grafana Dashboards

Create dashboards to visualize:

1. **Cache Hit Rate** by cache region
2. **Cache Miss Rate** trends over time
3. **Cache Size** per tenant
4. **Eviction Frequency** per cache
5. **Cache Latency** (Redis response time)

### Redis Monitoring Commands

```bash
# Connect to Redis
docker exec -it clinic-redis redis-cli

# Server information
INFO

# Memory usage
INFO memory

# Key statistics
INFO keyspace

# Monitor real-time commands
MONITOR

# Get specific key TTL
TTL "550e8400-e29b-41d4-a716-446655440000:users:list"

# Check key existence
EXISTS "550e8400-e29b-41d4-a716-446655440000:users:list"

# Get key type
TYPE "550e8400-e29b-41d4-a716-446655440000:users:list"

# Get key value
GET "550e8400-e29b-41d4-a716-446655440000:users:list"

# Delete specific key
DEL "550e8400-e29b-41d4-a716-446655440000:users:list"

# Clear all keys (DANGER - use with caution)
FLUSHALL
```

## Best Practices

### 1. Cache Only Expensive Operations

Cache operations that are:
- Database-intensive (complex joins, aggregations)
- Computationally expensive (calculations, transformations)
- High-frequency reads with low-frequency writes

**Don't cache:**
- Simple primary key lookups (already fast)
- Highly volatile data (more cache churn than benefit)
- User-specific data with low reuse (session data should use session store)

### 2. Use Appropriate TTL Values

- **Low volatility data**: 10-30 minutes
- **Medium volatility data**: 3-5 minutes
- **High volatility data**: 1-2 minutes
- **Real-time critical data**: Don't cache or use 30 seconds

### 3. Handle Cache Misses Gracefully

```java
@Cacheable(value = "users", key = "#tenantId + ':detail:' + #id")
public Optional<UserDetailViewDTO> getUserDetail(UUID id, UUID tenantId) {
    // Always return Optional to handle cache miss gracefully
    return userViewRepository.findDetailById(id, tenantId);
}
```

### 4. Avoid Caching Null Values

```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .disableCachingNullValues(); // Prevent caching null values
}
```

### 5. Use Conditional Caching

```java
// Don't cache empty results
@Cacheable(value = "patients",
           key = "#tenantId + ':search:' + #searchTerm",
           unless = "#result.isEmpty()")
public List<PatientListViewDTO> searchPatients(UUID tenantId, String searchTerm) {
    // Search logic
}
```

### 6. Monitor Cache Performance

Track:
- Hit rate (target: >80%)
- Miss rate (target: <20%)
- Eviction rate (high eviction = TTL too short or memory too low)
- Memory usage (target: <70% of max memory)

### 7. Plan for Cache Failures

```java
// Fallback to database if cache is unavailable
@Cacheable(value = "users", key = "#tenantId + ':list'")
public List<UserListViewDTO> getUserList(UUID tenantId) {
    try {
        return userViewRepository.findAllByTenantId(tenantId);
    } catch (Exception e) {
        log.error("Failed to fetch users from database", e);
        throw e;
    }
}
```

### 8. Use Cache Warming for Critical Data

```java
@Component
@RequiredArgsConstructor
public class CacheWarmer {

    private final UserService userService;
    private final TenantRepository tenantRepository;

    @Scheduled(fixedDelay = 600000) // Every 10 minutes
    public void warmRoleCache() {
        List<Tenant> tenants = tenantRepository.findAll();
        tenants.forEach(tenant -> {
            try {
                // Pre-populate cache with frequently accessed data
                userService.getUserList(tenant.getId());
            } catch (Exception e) {
                log.error("Failed to warm cache for tenant {}", tenant.getId(), e);
            }
        });
    }
}
```

## Troubleshooting

### Issue 1: Cache Not Working

**Symptoms**: Data not being cached, always hitting database

**Diagnosis**:
```bash
# Check if Redis is running
docker ps | grep clinic-redis

# Check Redis logs
docker logs clinic-redis

# Connect to Redis and check keys
docker exec -it clinic-redis redis-cli
KEYS *
```

**Solutions**:
1. Verify Redis is running and accessible
2. Check Spring configuration: `spring.cache.type=redis`
3. Ensure `@EnableCaching` annotation is present
4. Verify cache name matches configuration

### Issue 2: High Cache Miss Rate

**Symptoms**: Cache hit rate <50%

**Diagnosis**:
```bash
# Check cache statistics
curl http://localhost:8080/actuator/metrics/cache.gets

# Check TTL settings
docker exec -it clinic-redis redis-cli
TTL "{tenantId}:users:list"
```

**Solutions**:
1. Increase TTL for low-volatility data
2. Warm cache with frequently accessed data
3. Review cache eviction policies
4. Check if cache is being evicted too aggressively

### Issue 3: Memory Issues

**Symptoms**: Redis OOM errors, high memory usage

**Diagnosis**:
```bash
# Check Redis memory usage
docker exec -it clinic-redis redis-cli INFO memory

# Check number of keys
docker exec -it clinic-redis redis-cli DBSIZE

# Check largest keys
docker exec -it clinic-redis redis-cli --bigkeys
```

**Solutions**:
1. Reduce TTL values
2. Increase Redis memory allocation
3. Implement LRU eviction policy
4. Remove unused cache regions
5. Implement cache key namespacing

### Issue 4: Cross-Tenant Data Leakage

**Symptoms**: Users seeing data from other tenants

**Diagnosis**:
```bash
# Check cache keys - should all be tenant-prefixed
docker exec -it clinic-redis redis-cli KEYS *

# Verify tenant context is set
# Check application logs for TenantContext warnings
```

**Solutions**:
1. Ensure `TenantContext` is set in security filter
2. Verify `TenantAwareCacheKeyGenerator` is being used
3. Check all cache keys include tenant ID
4. Review JWT token extraction logic

### Issue 5: Serialization Errors

**Symptoms**: ClassCastException, serialization failures

**Diagnosis**:
```
# Check application logs for serialization errors
docker logs clinic-backend | grep SerializationException
```

**Solutions**:
1. Ensure all cached objects are serializable
2. Register custom serializers for complex types
3. Use `GenericJackson2JsonRedisSerializer` for JSON serialization
4. Add `@JsonTypeInfo` for polymorphic types

## Redis Configuration

### Docker Compose Configuration

```yaml
redis:
  image: redis:7-alpine
  container_name: clinic-redis
  command: redis-server --appendonly yes
  volumes:
    - redis_data:/data
  ports:
    - "6379:6379"
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 10s
    timeout: 5s
    retries: 5
  restart: unless-stopped
```

### Application Configuration

**application.yml**
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2

  cache:
    type: redis
```

### Production Configuration

For production, consider:

1. **Redis Cluster**: For high availability and scalability
2. **Persistence**: AOF + RDB for data durability
3. **Eviction Policy**: `maxmemory-policy allkeys-lru`
4. **Max Memory**: Set based on VM specs (e.g., 2GB for 32GB VM)
5. **Monitoring**: Redis Sentinel or Cluster monitoring

**redis.conf (production)**
```conf
# Memory management
maxmemory 2gb
maxmemory-policy allkeys-lru

# Persistence
appendonly yes
appendfsync everysec

# Replication (if using sentinel)
replica-read-only yes
replica-serve-stale-data yes

# Logging
loglevel notice
logfile /var/log/redis/redis.log
```

## References

- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Spring Data Redis](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Redis Documentation](https://redis.io/docs/)
- [Redis Best Practices](https://redis.io/docs/manual/patterns/)
- [ARCHITECTURE.md](ARCHITECTURE.md)
- [SECURITY.md](SECURITY.md)

---

**Document Version**: 1.0
**Last Updated**: 2026-01-16
**Maintained By**: Development Team
