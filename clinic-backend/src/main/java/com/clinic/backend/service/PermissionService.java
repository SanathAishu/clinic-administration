package com.clinic.backend.service;

import com.clinic.common.entity.core.Permission;
import com.clinic.backend.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

    private final PermissionRepository permissionRepository;

    /**
     * Create new permission (Bijective function: (resource, action) ↔ permission)
     * Permissions are shared across all tenants
     */
    @Transactional
    public Permission createPermission(Permission permission) {
        log.debug("Creating permission: {} on {}", permission.getAction(), permission.getResource());

        // Uniqueness validation (Bijective mapping)
        if (permissionRepository.existsByResourceAndAction(permission.getResource(), permission.getAction())) {
            throw new IllegalArgumentException(
                    String.format("Permission already exists: %s:%s",
                            permission.getResource(), permission.getAction()));
        }

        if (permission.getName() != null && permissionRepository.existsByName(permission.getName())) {
            throw new IllegalArgumentException("Permission name already exists: " + permission.getName());
        }

        Permission saved = permissionRepository.save(permission);
        log.info("Created permission: {} with ID: {}", saved.getName(), saved.getId());
        return saved;
    }

    /**
     * Get permission by ID
     */
    public Permission getPermissionById(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id));
    }

    /**
     * Get permission by resource and action (Bijective lookup)
     */
    public Permission getPermissionByResourceAndAction(String resource, String action) {
        return permissionRepository.findByResourceAndAction(resource, action)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Permission not found: %s:%s", resource, action)));
    }

    /**
     * Get permission by name
     */
    public Permission getPermissionByName(String name) {
        return permissionRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + name));
    }

    /**
     * Get all permissions (ordered)
     */
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAllOrdered();
    }

    /**
     * Get permissions by resource
     */
    public List<Permission> getPermissionsByResource(String resource) {
        return permissionRepository.findByResource(resource);
    }

    /**
     * Get permissions by action
     */
    public List<Permission> getPermissionsByAction(String action) {
        return permissionRepository.findByAction(action);
    }

    /**
     * Get permissions for role (Graph traversal: Role → Permissions)
     */
    public List<Permission> getPermissionsForRole(UUID roleId) {
        return permissionRepository.findByRoleId(roleId);
    }

    /**
     * Get permissions for user (Graph traversal: User → Roles → Permissions)
     * Returns the union of all permissions from all user's roles
     */
    public List<Permission> getPermissionsForUser(UUID userId, UUID tenantId) {
        return permissionRepository.findByUserIdAndTenantId(userId, tenantId);
    }

    /**
     * Check if user has permission
     */
    public boolean userHasPermission(UUID userId, UUID tenantId, String resource, String action) {
        List<Permission> userPermissions = getPermissionsForUser(userId, tenantId);

        // Set membership check
        return userPermissions.stream()
                .anyMatch(p -> p.getResource().equals(resource) && p.getAction().equals(action));
    }

    /**
     * Check if permission exists
     */
    public boolean permissionExists(String resource, String action) {
        return permissionRepository.existsByResourceAndAction(resource, action);
    }

    /**
     * Update permission
     */
    @Transactional
    public Permission updatePermission(UUID id, Permission updates) {
        Permission permission = getPermissionById(id);

        // Update resource/action (must maintain uniqueness)
        if (updates.getResource() != null || updates.getAction() != null) {
            String newResource = updates.getResource() != null ? updates.getResource() : permission.getResource();
            String newAction = updates.getAction() != null ? updates.getAction() : permission.getAction();

            if (!newResource.equals(permission.getResource()) || !newAction.equals(permission.getAction())) {
                if (permissionRepository.existsByResourceAndAction(newResource, newAction)) {
                    throw new IllegalArgumentException(
                            String.format("Permission already exists: %s:%s", newResource, newAction));
                }
                permission.setResource(newResource);
                permission.setAction(newAction);
            }
        }

        if (updates.getName() != null && !updates.getName().equals(permission.getName())) {
            if (permissionRepository.existsByName(updates.getName())) {
                throw new IllegalArgumentException("Permission name already exists: " + updates.getName());
            }
            permission.setName(updates.getName());
        }

        if (updates.getDescription() != null) {
            permission.setDescription(updates.getDescription());
        }

        Permission saved = permissionRepository.save(permission);
        log.info("Updated permission: {}", saved.getId());
        return saved;
    }

    /**
     * Delete permission (hard delete - permissions are not tenant-scoped)
     * WARNING: This will affect all tenants using this permission
     */
    @Transactional
    public void deletePermission(UUID id) {
        Permission permission = getPermissionById(id);

        // Check if permission is in use by any roles
        List<Permission> rolesUsingPermission = permissionRepository.findByRoleId(id);
        if (!rolesUsingPermission.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete permission that is assigned to roles. Remove from roles first.");
        }

        permissionRepository.delete(permission);
        log.info("Deleted permission: {}", id);
    }
}
