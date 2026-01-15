package com.clinic.backend.service;

import com.clinic.backend.repository.PermissionRepository;
import com.clinic.backend.repository.RoleRepository;
import com.clinic.backend.repository.RoleViewRepository;
import com.clinic.common.dto.view.RolePermissionsViewDTO;
import com.clinic.common.entity.core.Permission;
import com.clinic.common.entity.core.Role;
import com.clinic.common.entity.core.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleViewRepository roleViewRepository;
    private final PermissionRepository permissionRepository;

    /**
     * Create new role (Injective function within tenant: (name, tenantId) → role)
     */
    @Transactional
    public Role createRole(Role role, UUID tenantId) {
        log.debug("Creating role: {} for tenant: {}", role.getName(), tenantId);

        // Uniqueness validation (Set Theory)
        if (roleRepository.existsByNameAndTenantId(role.getName(), tenantId)) {
            throw new IllegalArgumentException("Role name already exists in this tenant: " + role.getName());
        }

        // Set tenant
        role.setTenantId(tenantId);

        // Set defaults
        if (role.getIsSystemRole() == null) {
            role.setIsSystemRole(false);
        }

        // Initialize collections if null
        if (role.getUsers() == null) {
            role.setUsers(new HashSet<>());
        }

        if (role.getPermissions() == null) {
            role.setPermissions(new HashSet<>());
        }

        Role saved = roleRepository.save(role);
        log.info("Created role: {} with ID: {}", saved.getName(), saved.getId());
        return saved;
    }

    /**
     * Create system role (shared across tenants)
     */
    @Transactional
    public Role createSystemRole(Role role) {
        log.debug("Creating system role: {}", role.getName());

        // Check if system role already exists
        if (roleRepository.findByNameAndIsSystemRole(role.getName(), true).isPresent()) {
            throw new IllegalArgumentException("System role already exists: " + role.getName());
        }

        role.setIsSystemRole(true);
        role.setTenantId(null); // System roles don't belong to any tenant

        if (role.getUsers() == null) {
            role.setUsers(new HashSet<>());
        }

        if (role.getPermissions() == null) {
            role.setPermissions(new HashSet<>());
        }

        Role saved = roleRepository.save(role);
        log.info("Created system role: {} with ID: {}", saved.getName(), saved.getId());
        return saved;
    }

    /**
     * Get role by ID (tenant-scoped)
     */
    public Role getRoleById(UUID id, UUID tenantId) {
        return roleRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
    }

    /**
     * Get role by name
     */
    public Role getRoleByName(String name, UUID tenantId) {
        return roleRepository.findByNameAndTenantId(name, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + name));
    }

    /**
     * Get all roles for tenant
     */
    public List<Role> getAllRolesForTenant(UUID tenantId) {
        return roleRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
    }

    /**
     * Get system roles
     */
    public List<Role> getSystemRoles() {
        return roleRepository.findByIsSystemRoleAndDeletedAtIsNull(true);
    }

    /**
     * Get roles for user (Graph traversal: User → Roles)
     */
    public List<Role> getRolesForUser(UUID userId, UUID tenantId) {
        return roleRepository.findByUserIdAndTenantId(userId, tenantId);
    }

    /**
     * Get roles with specific permission
     */
    public List<Role> getRolesWithPermission(UUID permissionId, UUID tenantId) {
        return roleRepository.findByPermissionIdAndTenantId(permissionId, tenantId);
    }

    /**
     * Update role
     */
    @Transactional
    public Role updateRole(UUID id, UUID tenantId, Role updates) {
        Role role = getRoleById(id, tenantId);

        // Cannot update system roles
        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new IllegalStateException("Cannot update system role: " + role.getName());
        }

        // Update allowed fields
        if (updates.getName() != null && !updates.getName().equals(role.getName())) {
            if (roleRepository.existsByNameAndTenantId(updates.getName(), tenantId)) {
                throw new IllegalArgumentException("Role name already exists: " + updates.getName());
            }
            role.setName(updates.getName());
        }

        if (updates.getDescription() != null) {
            role.setDescription(updates.getDescription());
        }

        Role saved = roleRepository.save(role);
        log.info("Updated role: {}", saved.getId());
        return saved;
    }

    /**
     * Add permission to role (Set operation: add element)
     */
    @Transactional
    public Role addPermissionToRole(UUID roleId, UUID tenantId, Permission permission) {
        Role role = getRoleById(roleId, tenantId);

        // Cannot modify system roles
        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new IllegalStateException("Cannot modify system role: " + role.getName());
        }

        // Add permission (Set Theory: element addition is idempotent)
        role.getPermissions().add(permission);

        Role saved = roleRepository.save(role);
        log.info("Added permission {} to role {}", permission.getName(), saved.getName());
        return saved;
    }

    /**
     * Remove permission from role (Set operation: remove element)
     */
    @Transactional
    public Role removePermissionFromRole(UUID roleId, UUID tenantId, Permission permission) {
        Role role = getRoleById(roleId, tenantId);

        // Cannot modify system roles
        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new IllegalStateException("Cannot modify system role: " + role.getName());
        }

        // Remove permission (Set Theory: removal is idempotent)
        role.getPermissions().remove(permission);

        Role saved = roleRepository.save(role);
        log.info("Removed permission {} from role {}", permission.getName(), saved.getName());
        return saved;
    }

    /**
     * Set permissions for role (Set replacement)
     */
    @Transactional
    public Role setRolePermissions(UUID roleId, UUID tenantId, Set<Permission> permissions) {
        Role role = getRoleById(roleId, tenantId);

        // Cannot modify system roles
        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new IllegalStateException("Cannot modify system role: " + role.getName());
        }

        // Replace permission set
        role.setPermissions(permissions);

        Role saved = roleRepository.save(role);
        log.info("Set {} permissions for role {}", permissions.size(), saved.getName());
        return saved;
    }

    /**
     * Assign role to user (Set operation)
     */
    @Transactional
    public void assignRoleToUser(UUID roleId, UUID tenantId, User user) {
        Role role = getRoleById(roleId, tenantId);

        // Add user to role's user set (bidirectional)
        role.getUsers().add(user);
        user.getRoles().add(role);

        roleRepository.save(role);
        log.info("Assigned role {} to user {}", role.getName(), user.getEmail());
    }

    /**
     * Remove role from user (Set operation)
     */
    @Transactional
    public void removeRoleFromUser(UUID roleId, UUID tenantId, User user) {
        Role role = getRoleById(roleId, tenantId);

        // Remove user from role's user set (bidirectional)
        role.getUsers().remove(user);
        user.getRoles().remove(role);

        roleRepository.save(role);
        log.info("Removed role {} from user {}", role.getName(), user.getEmail());
    }

    /**
     * Soft delete role
     */
    @Transactional
    public void softDeleteRole(UUID roleId, UUID tenantId) {
        Role role = getRoleById(roleId, tenantId);

        // Cannot delete system roles
        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new IllegalStateException("Cannot delete system role: " + role.getName());
        }

        role.softDelete();
        roleRepository.save(role);
        log.info("Soft deleted role: {}", roleId);
    }

    /**
     * Count roles for tenant
     */
    public long countRolesForTenant(UUID tenantId) {
        return roleRepository.countByTenantIdAndDeletedAtIsNull(tenantId);
    }

    // ========================================================================
    // CQRS READ OPERATIONS (using database views)
    // ========================================================================

    /**
     * Get role list using v_role_permissions view (CQRS Read)
     * Returns roles with pre-aggregated permission information.
     */
    public List<RolePermissionsViewDTO> getRoleListView(UUID tenantId) {
        return roleViewRepository.findAllByTenantId(tenantId);
    }

    /**
     * Get role detail using v_role_permissions view (CQRS Read)
     * Returns complete role profile with permissions.
     */
    public Optional<RolePermissionsViewDTO> getRoleDetailView(UUID id, UUID tenantId) {
        return roleViewRepository.findDetailById(id, tenantId);
    }

    /**
     * Get system roles using view (CQRS Read)
     */
    public List<RolePermissionsViewDTO> getSystemRolesView() {
        return roleViewRepository.findSystemRoles();
    }

    /**
     * Get tenant-specific roles using view (CQRS Read)
     */
    public List<RolePermissionsViewDTO> getTenantRolesView(UUID tenantId) {
        return roleViewRepository.findTenantRoles(tenantId);
    }

    /**
     * Search roles using view (CQRS Read)
     */
    public List<RolePermissionsViewDTO> searchRolesView(UUID tenantId, String searchTerm) {
        return roleViewRepository.searchByTenantId(tenantId, searchTerm);
    }

    // ========================================================================
    // PERMISSION ASSIGNMENT OPERATIONS
    // ========================================================================

    /**
     * Assign permissions to role by IDs (Set operation: union)
     */
    @Transactional
    public Role assignPermissionsToRole(UUID roleId, UUID tenantId, Set<UUID> permissionIds) {
        Role role = getRoleById(roleId, tenantId);

        // Cannot modify system roles
        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new IllegalStateException("Cannot modify system role: " + role.getName());
        }

        // Validate all permissions exist
        Set<Permission> permissionsToAssign = permissionIds.stream()
                .map(permId -> permissionRepository.findById(permId)
                        .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permId)))
                .collect(Collectors.toSet());

        // Add permissions (Set union - idempotent)
        role.getPermissions().addAll(permissionsToAssign);

        Role saved = roleRepository.save(role);
        log.info("Assigned {} permissions to role {}", permissionsToAssign.size(), roleId);
        return saved;
    }

    /**
     * Remove permission from role by ID
     */
    @Transactional
    public Role removePermissionFromRoleById(UUID roleId, UUID tenantId, UUID permissionId) {
        Role role = getRoleById(roleId, tenantId);

        // Cannot modify system roles
        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new IllegalStateException("Cannot modify system role: " + role.getName());
        }

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionId));

        // Remove permission (Set difference - idempotent)
        boolean removed = role.getPermissions().remove(permission);
        if (!removed) {
            log.debug("Permission {} was not assigned to role {}", permissionId, roleId);
        }

        Role saved = roleRepository.save(role);
        log.info("Removed permission {} from role {}", permissionId, roleId);
        return saved;
    }

    /**
     * Replace all role permissions by IDs (Set replacement)
     */
    @Transactional
    public Role setRolePermissionsByIds(UUID roleId, UUID tenantId, Set<UUID> permissionIds) {
        Role role = getRoleById(roleId, tenantId);

        // Cannot modify system roles
        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new IllegalStateException("Cannot modify system role: " + role.getName());
        }

        // Validate all permissions exist
        Set<Permission> newPermissions = permissionIds.stream()
                .map(permId -> permissionRepository.findById(permId)
                        .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permId)))
                .collect(Collectors.toSet());

        // Replace permission set
        role.getPermissions().clear();
        role.getPermissions().addAll(newPermissions);

        Role saved = roleRepository.save(role);
        log.info("Set {} permissions for role {}", newPermissions.size(), roleId);
        return saved;
    }

    /**
     * Get role by ID, allowing system roles regardless of tenant
     */
    public Role getRoleByIdIncludingSystemRoles(UUID id, UUID tenantId) {
        return roleRepository.findById(id)
                .filter(role -> role.getDeletedAt() == null)
                .filter(role -> Boolean.TRUE.equals(role.getIsSystemRole()) ||
                               tenantId.equals(role.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
    }
}
