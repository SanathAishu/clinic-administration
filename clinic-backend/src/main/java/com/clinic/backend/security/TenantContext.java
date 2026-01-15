package com.clinic.backend.security;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Thread-local storage for current tenant ID.
 * Used for Row Level Security (RLS) in PostgreSQL.
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Private constructor to prevent instantiation
    }

    /**
     * Set the current tenant ID
     */
    public static void setCurrentTenant(UUID tenantId) {
        log.debug("Setting tenant context: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Get the current tenant ID
     */
    public static UUID getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    /**
     * Clear the tenant context
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }

    /**
     * Check if tenant context is set
     */
    public static boolean isSet() {
        return CURRENT_TENANT.get() != null;
    }
}
