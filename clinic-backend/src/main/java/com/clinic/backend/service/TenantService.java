package com.clinic.backend.service;

import com.clinic.common.entity.core.Tenant;
import com.clinic.backend.repository.TenantRepository;
import com.clinic.common.enums.TenantStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantService {

    private final TenantRepository tenantRepository;

    /**
     * Create new tenant (Injective function: subdomain → tenant)
     * Enforces uniqueness constraints
     */
    @Transactional
    public Tenant createTenant(Tenant tenant) {
        log.debug("Creating tenant with subdomain: {}", tenant.getSubdomain());

        // Uniqueness validation (Set Theory)
        if (tenantRepository.existsBySubdomain(tenant.getSubdomain())) {
            throw new IllegalArgumentException("Subdomain already exists: " + tenant.getSubdomain());
        }

        if (tenantRepository.existsByEmail(tenant.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + tenant.getEmail());
        }

        // Set default values
        if (tenant.getStatus() == null) {
            tenant.setStatus(TenantStatus.TRIAL);
        }

        if (tenant.getSubscriptionStartDate() == null) {
            tenant.setSubscriptionStartDate(LocalDate.now());
        }

        // Cardinality constraints (default capacity)
        if (tenant.getMaxUsers() == null) {
            tenant.setMaxUsers(10);
        }

        if (tenant.getMaxPatients() == null) {
            tenant.setMaxPatients(500);
        }

        Tenant saved = tenantRepository.save(tenant);
        log.info("Created tenant: {} with ID: {}", saved.getSubdomain(), saved.getId());
        return saved;
    }

    /**
     * Get tenant by ID
     */
    public Tenant getTenantById(UUID id) {
        return tenantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
    }

    /**
     * Get tenant by subdomain (Injective lookup)
     */
    public Tenant getTenantBySubdomain(String subdomain) {
        return tenantRepository.findBySubdomainAndDeletedAtIsNull(subdomain)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + subdomain));
    }

    /**
     * Update tenant
     */
    @Transactional
    public Tenant updateTenant(UUID id, Tenant updates) {
        Tenant tenant = getTenantById(id);

        // Update only allowed fields
        if (updates.getName() != null) {
            tenant.setName(updates.getName());
        }

        if (updates.getEmail() != null && !updates.getEmail().equals(tenant.getEmail())) {
            if (tenantRepository.existsByEmail(updates.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + updates.getEmail());
            }
            tenant.setEmail(updates.getEmail());
        }

        if (updates.getPhone() != null) {
            tenant.setPhone(updates.getPhone());
        }

        if (updates.getAddress() != null) {
            tenant.setAddress(updates.getAddress());
        }

        if (updates.getMaxUsers() != null) {
            tenant.setMaxUsers(updates.getMaxUsers());
        }

        if (updates.getMaxPatients() != null) {
            tenant.setMaxPatients(updates.getMaxPatients());
        }

        Tenant saved = tenantRepository.save(tenant);
        log.info("Updated tenant: {}", saved.getId());
        return saved;
    }

    /**
     * Update tenant status (State machine)
     */
    @Transactional
    public Tenant updateTenantStatus(UUID id, TenantStatus newStatus) {
        Tenant tenant = getTenantById(id);

        // State transition validation
        validateStatusTransition(tenant.getStatus(), newStatus);

        tenant.setStatus(newStatus);

        if (newStatus == TenantStatus.SUSPENDED || newStatus == TenantStatus.EXPIRED) {
            log.warn("Tenant {} status changed to: {}", tenant.getSubdomain(), newStatus);
        }

        Tenant saved = tenantRepository.save(tenant);
        log.info("Updated tenant status: {} -> {}", saved.getId(), newStatus);
        return saved;
    }

    /**
     * Extend subscription
     */
    @Transactional
    public Tenant extendSubscription(UUID id, LocalDate newEndDate) {
        Tenant tenant = getTenantById(id);

        // Temporal validation: new end date must be after current
        if (tenant.getSubscriptionEndDate() != null &&
            newEndDate.isBefore(tenant.getSubscriptionEndDate())) {
            throw new IllegalArgumentException("New subscription end date cannot be before current end date");
        }

        tenant.setSubscriptionEndDate(newEndDate);

        // Reactivate if was expired
        if (tenant.getStatus() == TenantStatus.EXPIRED) {
            tenant.setStatus(TenantStatus.ACTIVE);
        }

        Tenant saved = tenantRepository.save(tenant);
        log.info("Extended subscription for tenant: {} until: {}", saved.getId(), newEndDate);
        return saved;
    }

    /**
     * Get all active tenants
     */
    public List<Tenant> getAllActiveTenants() {
        return tenantRepository.findAllActive();
    }

    /**
     * Get tenants by status
     */
    public List<Tenant> getTenantsByStatus(TenantStatus status) {
        return tenantRepository.findByStatusAndDeletedAtIsNull(status);
    }

    /**
     * Get expired subscriptions
     */
    public List<Tenant> getExpiredSubscriptions() {
        return tenantRepository.findExpiredSubscriptions(LocalDate.now(), TenantStatus.ACTIVE);
    }

    /**
     * Get subscriptions expiring soon (within next 30 days)
     */
    public List<Tenant> getExpiringSubscriptions() {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(30);
        return tenantRepository.findExpiringSubscriptions(today, futureDate);
    }

    /**
     * Soft delete tenant (Idempotent operation)
     */
    @Transactional
    public void softDeleteTenant(UUID id) {
        Tenant tenant = getTenantById(id);
        tenant.softDelete();
        tenantRepository.save(tenant);
        log.info("Soft deleted tenant: {}", id);
    }

    /**
     * Check if subdomain is available
     */
    public boolean isSubdomainAvailable(String subdomain) {
        return !tenantRepository.existsBySubdomain(subdomain);
    }

    /**
     * Validate state transition (Discrete Math: State machine with valid transitions)
     */
    private void validateStatusTransition(TenantStatus current, TenantStatus next) {
        // Valid transitions (DAG)
        boolean valid = switch (current) {
            case TRIAL -> next == TenantStatus.ACTIVE || next == TenantStatus.SUSPENDED ||
                          next == TenantStatus.EXPIRED;
            case ACTIVE -> next == TenantStatus.SUSPENDED || next == TenantStatus.EXPIRED;
            case SUSPENDED -> next == TenantStatus.ACTIVE || next == TenantStatus.EXPIRED;
            case EXPIRED -> next == TenantStatus.ACTIVE; // Can be reactivated
        };

        if (!valid) {
            throw new IllegalStateException(
                    String.format("Invalid status transition: %s -> %s", current, next));
        }
    }
}
