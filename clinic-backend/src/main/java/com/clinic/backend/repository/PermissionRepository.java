package com.clinic.backend.repository;

import com.clinic.backend.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    // Uniqueness enforcement (resource + action combination)
    Optional<Permission> findByResourceAndAction(String resource, String action);

    Optional<Permission> findByName(String name);

    boolean existsByResourceAndAction(String resource, String action);

    boolean existsByName(String name);

    // Resource-based queries
    List<Permission> findByResource(String resource);

    List<Permission> findByResourceIn(List<String> resources);

    // Action-based queries
    List<Permission> findByAction(String action);

    // Permission lookup by role
    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.id = :roleId")
    List<Permission> findByRoleId(@Param("roleId") UUID roleId);

    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.id = :roleId AND r.tenantId = :tenantId")
    List<Permission> findByRoleIdAndTenantId(@Param("roleId") UUID roleId, @Param("tenantId") UUID tenantId);

    // Permission lookup by user (through roles)
    @Query("SELECT DISTINCT p FROM Permission p JOIN p.roles r JOIN r.users u WHERE u.id = :userId AND u.tenantId = :tenantId AND u.deletedAt IS NULL")
    List<Permission> findByUserIdAndTenantId(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    // All permissions ordered by resource
    @Query("SELECT p FROM Permission p ORDER BY p.resource, p.action")
    List<Permission> findAllOrdered();
}
