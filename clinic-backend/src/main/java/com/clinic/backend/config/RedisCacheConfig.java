package com.clinic.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis distributed cache configuration for Spring Boot 3.x multi-tenant application.
 *
 * <p>Implements {@link CachingConfigurer} directly (NOT CachingConfigurerSupport which is deprecated in Spring 6.0).
 *
 * <p>Multi-tenancy Support:
 * All cache keys MUST include tenantId to ensure data isolation between tenants.
 * Use {@link TenantAwareCacheKeyGenerator} to automatically prepend tenant ID to all cache keys.
 *
 * <p>Cache Naming Strategy:
 * <ul>
 *   <li><b>roles</b> - User roles and permissions (low volatility) - 10 minutes TTL</li>
 *   <li><b>users</b> - User entities and authentication data (low volatility) - 10 minutes TTL</li>
 *   <li><b>patients</b> - Patient demographic and profile data (medium volatility) - 5 minutes TTL</li>
 *   <li><b>appointments</b> - Appointment schedules and bookings (high volatility) - 2 minutes TTL</li>
 *   <li><b>appointments:today</b> - Today's appointments (very high volatility) - 1 minute TTL</li>
 *   <li><b>billings</b> - Billing records and payment status (financial data) - 3 minutes TTL</li>
 *   <li><b>billings:summary</b> - Historical billing summaries (low volatility) - 10 minutes TTL</li>
 * </ul>
 *
 * <p>Cache Key Format:
 * Format: "{tenantId}:{cacheName}:{methodParams}"
 * <pre>
 * Example: "550e8400-e29b-41d4-a716-446655440000:users:123e4567-e89b-12d3-a456-426614174000"
 * </pre>
 *
 * <p>Serialization:
 * Uses {@link GenericJackson2JsonRedisSerializer} for proper serialization of complex Java objects,
 * including Java 8 date/time types and custom entities.
 *
 * @see CachingConfigurer
 * @see TenantAwareCacheKeyGenerator
 * @see org.springframework.cache.annotation.Cacheable
 * @see org.springframework.cache.annotation.CacheEvict
 * @see org.springframework.cache.annotation.CachePut
 */
@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    /**
     * Cache names as constants for type-safe reference.
     */
    public static final String ROLES_CACHE = "roles";
    public static final String USERS_CACHE = "users";
    public static final String PATIENTS_CACHE = "patients";
    public static final String APPOINTMENTS_CACHE = "appointments";
    public static final String APPOINTMENTS_TODAY_CACHE = "appointments:today";
    public static final String BILLINGS_CACHE = "billings";
    public static final String BILLINGS_SUMMARY_CACHE = "billings:summary";

    /**
     * TTL configuration (based on data volatility).
     */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    private static final Duration ROLES_TTL = Duration.ofMinutes(10);
    private static final Duration USERS_TTL = Duration.ofMinutes(10);
    private static final Duration PATIENTS_TTL = Duration.ofMinutes(5);
    private static final Duration APPOINTMENTS_TTL = Duration.ofMinutes(2);
    private static final Duration APPOINTMENTS_TODAY_TTL = Duration.ofMinutes(1);
    private static final Duration BILLINGS_TTL = Duration.ofMinutes(3);
    private static final Duration BILLINGS_SUMMARY_TTL = Duration.ofMinutes(10);

    /**
     * Configures RedisCacheManager with per-cache TTL settings.
     *
     * <p>Cache Characteristics:
     * <ul>
     *   <li><b>Eviction Policy</b>: Time-based (TTL) + Redis maxmemory-policy</li>
     *   <li><b>Serialization</b>: JSON via GenericJackson2JsonRedisSerializer</li>
     *   <li><b>Statistics</b>: Enabled for monitoring via Spring Boot Actuator</li>
     *   <li><b>Key Prefix</b>: Includes tenant context for multi-tenancy isolation</li>
     *   <li><b>Null Values</b>: Disabled to prevent caching of non-existent entities</li>
     * </ul>
     *
     * @param connectionFactory Redis connection factory (auto-configured by Spring Boot)
     * @return configured RedisCacheManager instance
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(createJsonSerializer()))
                .disableCachingNullValues(); // Fail-fast on missing data

        // Per-cache TTL configuration
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(ROLES_CACHE, defaultConfig.entryTtl(ROLES_TTL));
        cacheConfigurations.put(USERS_CACHE, defaultConfig.entryTtl(USERS_TTL));
        cacheConfigurations.put(PATIENTS_CACHE, defaultConfig.entryTtl(PATIENTS_TTL));
        cacheConfigurations.put(APPOINTMENTS_CACHE, defaultConfig.entryTtl(APPOINTMENTS_TTL));
        cacheConfigurations.put(APPOINTMENTS_TODAY_CACHE, defaultConfig.entryTtl(APPOINTMENTS_TODAY_TTL));
        cacheConfigurations.put(BILLINGS_CACHE, defaultConfig.entryTtl(BILLINGS_TTL));
        cacheConfigurations.put(BILLINGS_SUMMARY_CACHE, defaultConfig.entryTtl(BILLINGS_SUMMARY_TTL));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .enableStatistics() // Enable cache statistics for monitoring
                .build();
    }

    /**
     * Provides the tenant-aware key generator for cache operations.
     *
     * <p>This key generator automatically prepends the tenant ID to all cache keys,
     * ensuring data isolation between tenants in a multi-tenant environment.
     *
     * @return TenantAwareCacheKeyGenerator instance
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return new TenantAwareCacheKeyGenerator();
    }

    /**
     * Creates a GenericJackson2JsonRedisSerializer with custom ObjectMapper configuration.
     *
     * <p>Configuration includes:
     * <ul>
     *   <li>JavaTimeModule for Java 8 date/time serialization</li>
     *   <li>Type information for polymorphic deserialization</li>
     *   <li>LaissezFaireSubTypeValidator for flexible type handling</li>
     * </ul>
     *
     * @return configured GenericJackson2JsonRedisSerializer
     */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Register JavaTimeModule for proper Java 8 date/time serialization
        objectMapper.registerModule(new JavaTimeModule());

        // Enable default typing for polymorphic types
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
