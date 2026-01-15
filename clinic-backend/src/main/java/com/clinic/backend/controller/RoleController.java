package com.clinic.backend.controller;

import com.clinic.backend.mapper.RoleMapper;
import com.clinic.backend.service.RoleService;
import com.clinic.common.dto.request.CreateRoleRequest;
import com.clinic.common.dto.request.UpdateRoleRequest;
import com.clinic.common.dto.response.RoleResponseDTO;
import com.clinic.common.dto.view.RolePermissionsViewDTO;
import com.clinic.common.entity.core.Role;
import com.clinic.backend.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * REST Controller for Role management.
 *
 * Implements CQRS pattern:
 * - READ operations: Use database views (v_role_permissions) for optimized reads
 * - WRITE operations: Use JPA entities for proper relationship management
 *
 * All operations are tenant-scoped using TenantContext.
 * System roles (is_system_role = true) are shared across tenants.
 */
@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Role management endpoints")
public class RoleController {

    private final RoleService roleService;
    private final RoleMapper roleMapper;

    // ========================================================================
    // READ ENDPOINTS (CQRS: using database views)
    // ========================================================================

    /**
     * List all roles for the current tenant.
     * Uses v_role_permissions view for optimized read with pre-aggregated permission information.
     * Includes both tenant-specific roles and system roles.
     */
    @GetMapping
    @Operation(summary = "List all roles", description = "Get all roles (tenant-specific and system) with aggregated permission information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved role list"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<RolePermissionsViewDTO>> listRoles() {
        UUID tenantId = getTenantId();
        log.debug("Listing roles for tenant: {}", tenantId);

        List<RolePermissionsViewDTO> roles = roleService.getRoleListView(tenantId);
        return ResponseEntity.ok(roles);
    }

    /**
     * Get role details by ID.
     * Uses v_role_permissions view for complete role profile with permissions.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get role details", description = "Get complete role profile with permissions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved role details"),
            @ApiResponse(responseCode = "404", description = "Role not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<RolePermissionsViewDTO> getRoleDetail(
            @Parameter(description = "Role ID") @PathVariable UUID id) {
        UUID tenantId = getTenantId();
        log.debug("Getting role detail: {} for tenant: {}", id, tenantId);

        return roleService.getRoleDetailView(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List system roles only.
     * System roles are shared across all tenants.
     */
    @GetMapping("/system")
    @Operation(summary = "List system roles", description = "Get all system roles (shared across tenants)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved system roles"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<RolePermissionsViewDTO>> listSystemRoles() {
        log.debug("Listing system roles");

        List<RolePermissionsViewDTO> roles = roleService.getSystemRolesView();
        return ResponseEntity.ok(roles);
    }

    /**
     * List tenant-specific roles only (excluding system roles).
     */
    @GetMapping("/tenant")
    @Operation(summary = "List tenant roles", description = "Get all tenant-specific roles (excluding system roles)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tenant roles"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<RolePermissionsViewDTO>> listTenantRoles() {
        UUID tenantId = getTenantId();
        log.debug("Listing tenant-specific roles for tenant: {}", tenantId);

        List<RolePermissionsViewDTO> roles = roleService.getTenantRolesView(tenantId);
        return ResponseEntity.ok(roles);
    }

    /**
     * Search roles by name or description.
     */
    @GetMapping("/search")
    @Operation(summary = "Search roles", description = "Search roles by name or description (case-insensitive)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved search results"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<RolePermissionsViewDTO>> searchRoles(
            @Parameter(description = "Search term for name or description") @RequestParam("q") String searchTerm) {
        UUID tenantId = getTenantId();
        log.debug("Searching roles with term '{}' for tenant: {}", searchTerm, tenantId);

        List<RolePermissionsViewDTO> roles = roleService.searchRolesView(tenantId, searchTerm);
        return ResponseEntity.ok(roles);
    }

    // ========================================================================
    // WRITE ENDPOINTS (CQRS: using JPA entities)
    // ========================================================================

    /**
     * Create a new role.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create role", description = "Create a new tenant-specific role (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Role created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "409", description = "Role name already exists")
    })
    public ResponseEntity<RoleResponseDTO> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        UUID tenantId = getTenantId();
        log.info("Creating role: {} for tenant: {}", request.getName(), tenantId);

        // Map request to entity
        Role role = roleMapper.toEntity(request);

        // Create role
        Role created = roleService.createRole(role, tenantId);

        // Assign permissions if provided
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            created = roleService.assignPermissionsToRole(created.getId(), tenantId, request.getPermissionIds());
        }

        RoleResponseDTO response = roleMapper.toResponseDTO(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing role.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update role", description = "Update role details (Admin only). Cannot update system roles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or attempt to modify system role"),
            @ApiResponse(responseCode = "404", description = "Role not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "409", description = "Role name already exists")
    })
    public ResponseEntity<RoleResponseDTO> updateRole(
            @Parameter(description = "Role ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        UUID tenantId = getTenantId();
        log.info("Updating role: {} for tenant: {}", id, tenantId);

        // Get existing role
        Role existing = roleService.getRoleById(id, tenantId);

        // Apply updates
        roleMapper.updateEntityFromRequest(request, existing);

        // Save updates
        Role updated = roleService.updateRole(id, tenantId, existing);

        // Update permissions if provided
        if (request.getPermissionIds() != null) {
            updated = roleService.setRolePermissionsByIds(id, tenantId, request.getPermissionIds());
        }

        RoleResponseDTO response = roleMapper.toResponseDTO(updated);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a role (soft delete).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete role", description = "Soft delete a role (Admin only). Cannot delete system roles.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Role deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Attempt to delete system role"),
            @ApiResponse(responseCode = "404", description = "Role not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<Void> deleteRole(
            @Parameter(description = "Role ID") @PathVariable UUID id) {
        UUID tenantId = getTenantId();
        log.info("Deleting role: {} for tenant: {}", id, tenantId);

        roleService.softDeleteRole(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Assign permissions to a role.
     */
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign permissions", description = "Assign permissions to a role (Admin only). Cannot modify system roles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permissions assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid permission IDs or attempt to modify system role"),
            @ApiResponse(responseCode = "404", description = "Role or permission not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<RoleResponseDTO> assignPermissions(
            @Parameter(description = "Role ID") @PathVariable UUID id,
            @RequestBody Set<UUID> permissionIds) {
        UUID tenantId = getTenantId();
        log.info("Assigning {} permissions to role: {} for tenant: {}",
                permissionIds.size(), id, tenantId);

        Role updated = roleService.assignPermissionsToRole(id, tenantId, permissionIds);
        RoleResponseDTO response = roleMapper.toResponseDTO(updated);

        return ResponseEntity.ok(response);
    }

    /**
     * Remove a permission from a role.
     */
    @DeleteMapping("/{id}/permissions/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove permission", description = "Remove a permission from a role (Admin only). Cannot modify system roles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permission removed successfully"),
            @ApiResponse(responseCode = "400", description = "Attempt to modify system role"),
            @ApiResponse(responseCode = "404", description = "Role or permission not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<RoleResponseDTO> removePermission(
            @Parameter(description = "Role ID") @PathVariable UUID id,
            @Parameter(description = "Permission ID to remove") @PathVariable UUID permissionId) {
        UUID tenantId = getTenantId();
        log.info("Removing permission: {} from role: {} for tenant: {}", permissionId, id, tenantId);

        Role updated = roleService.removePermissionFromRoleById(id, tenantId, permissionId);
        RoleResponseDTO response = roleMapper.toResponseDTO(updated);

        return ResponseEntity.ok(response);
    }

    /**
     * Replace all permissions for a role.
     */
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set permissions", description = "Replace all permissions for a role (Admin only). Cannot modify system roles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permissions set successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid permission IDs or attempt to modify system role"),
            @ApiResponse(responseCode = "404", description = "Role or permission not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<RoleResponseDTO> setPermissions(
            @Parameter(description = "Role ID") @PathVariable UUID id,
            @RequestBody Set<UUID> permissionIds) {
        UUID tenantId = getTenantId();
        log.info("Setting {} permissions for role: {} in tenant: {}",
                permissionIds.size(), id, tenantId);

        Role updated = roleService.setRolePermissionsByIds(id, tenantId, permissionIds);
        RoleResponseDTO response = roleMapper.toResponseDTO(updated);

        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Get current tenant ID from context.
     * Throws IllegalStateException if tenant context is not set.
     */
    private UUID getTenantId() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not set");
        }
        return tenantId;
    }
}
