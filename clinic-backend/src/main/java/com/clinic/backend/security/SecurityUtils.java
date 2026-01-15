package com.clinic.backend.security;

import com.clinic.common.security.TenantContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility class for accessing security context information.
 * Provides consistent access to current user details across controllers.
 *
 * Usage:
 *   UUID userId = SecurityUtils.getCurrentUserId();
 *   UUID tenantId = SecurityUtils.getCurrentTenantId();
 *   UserPrincipal user = SecurityUtils.getCurrentUser().orElseThrow();
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Get the current authentication from SecurityContext.
     */
    public static Optional<Authentication> getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(authentication);
    }

    /**
     * Get the current UserPrincipal from SecurityContext.
     */
    public static Optional<UserPrincipal> getCurrentUser() {
        return getAuthentication()
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof UserPrincipal)
                .map(principal -> (UserPrincipal) principal);
    }

    /**
     * Get current user ID (throws if not authenticated).
     */
    public static UUID getCurrentUserId() {
        return getCurrentUser()
                .map(UserPrincipal::getId)
                .orElseThrow(() -> new SecurityException("No authenticated user found"));
    }

    /**
     * Get current user ID as Optional (safe version).
     */
    public static Optional<UUID> getCurrentUserIdOptional() {
        return getCurrentUser().map(UserPrincipal::getId);
    }

    /**
     * Get current tenant ID (throws if not authenticated).
     * Uses UserPrincipal first, falls back to TenantContext.
     */
    public static UUID getCurrentTenantId() {
        return getCurrentUser()
                .map(UserPrincipal::getTenantId)
                .or(() -> Optional.ofNullable(TenantContext.getCurrentTenant()))
                .orElseThrow(() -> new SecurityException("No tenant context found"));
    }

    /**
     * Get current tenant ID as Optional (safe version).
     */
    public static Optional<UUID> getCurrentTenantIdOptional() {
        return getCurrentUser()
                .map(UserPrincipal::getTenantId)
                .or(() -> Optional.ofNullable(TenantContext.getCurrentTenant()));
    }

    /**
     * Get current user's email.
     */
    public static Optional<String> getCurrentUserEmail() {
        return getCurrentUser().map(UserPrincipal::getEmail);
    }

    /**
     * Get current user's full name.
     */
    public static Optional<String> getCurrentUserFullName() {
        return getCurrentUser().map(UserPrincipal::getFullName);
    }

    /**
     * Get current user's roles.
     */
    public static List<String> getCurrentUserRoles() {
        return getCurrentUser()
                .map(UserPrincipal::getRoles)
                .orElse(Collections.emptyList());
    }

    /**
     * Get current user's permissions.
     */
    public static List<String> getCurrentUserPermissions() {
        return getCurrentUser()
                .map(UserPrincipal::getPermissions)
                .orElse(Collections.emptyList());
    }

    /**
     * Get all authorities (roles + permissions) as strings.
     */
    public static List<String> getCurrentUserAuthorities() {
        return getAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    /**
     * Check if current user is authenticated.
     */
    public static boolean isAuthenticated() {
        return getAuthentication().isPresent();
    }

    /**
     * Check if current user has a specific role.
     */
    public static boolean hasRole(String role) {
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals(roleWithPrefix)))
                .orElse(false);
    }

    /**
     * Check if current user has any of the specified roles.
     */
    public static boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if current user has a specific permission.
     */
    public static boolean hasPermission(String permission) {
        return getAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals(permission)))
                .orElse(false);
    }

    /**
     * Check if current user is an admin.
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Check if current user is a doctor.
     */
    public static boolean isDoctor() {
        return hasRole("DOCTOR");
    }

    /**
     * Require authentication - throws if not authenticated.
     */
    public static UserPrincipal requireAuthentication() {
        return getCurrentUser()
                .orElseThrow(() -> new SecurityException("Authentication required"));
    }

    /**
     * Require specific role - throws if user doesn't have the role.
     */
    public static void requireRole(String role) {
        if (!hasRole(role)) {
            throw new SecurityException("Required role not present: " + role);
        }
    }

    /**
     * Require any of the specified roles - throws if user doesn't have any.
     */
    public static void requireAnyRole(String... roles) {
        if (!hasAnyRole(roles)) {
            throw new SecurityException("Required roles not present: " + String.join(", ", roles));
        }
    }
}
