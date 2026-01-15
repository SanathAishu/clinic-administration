package com.clinic.backend.repository;

import com.clinic.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    // Uniqueness enforcement per tenant
    Optional<Role> findByNameAndTenantId(String name, UUID tenantId);

    boolean existsByNameAndTenantId(String name, UUID tenantId);

    // Tenant-scoped queries
    List<Role> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    // System roles (shared across tenants)
    List<Role> findByIsSystemRoleAndDeletedAtIsNull(Boolean isSystemRole);

    Optional<Role> findByNameAndIsSystemRole(String name, Boolean isSystemRole);

    // Role hierarchy queries
    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.id = :userId AND r.tenantId = :tenantId AND r.deletedAt IS NULL")
    List<Role> findByUserIdAndTenantId(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.id = :permissionId AND r.tenantId = :tenantId AND r.deletedAt IS NULL")
    List<Role> findByPermissionIdAndTenantId(@Param("permissionId") UUID permissionId, @Param("tenantId") UUID tenantId);

    // Soft delete support
    Optional<Role> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // Count roles for tenant
    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);
}
