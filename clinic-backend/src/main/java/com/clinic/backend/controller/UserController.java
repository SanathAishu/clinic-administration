package com.clinic.backend.controller;

import com.clinic.backend.mapper.UserMapper;
import com.clinic.backend.service.UserService;
import com.clinic.common.dto.request.AssignRolesRequest;
import com.clinic.common.dto.request.CreateUserRequest;
import com.clinic.common.dto.request.ResetPasswordRequest;
import com.clinic.common.dto.request.UpdateUserRequest;
import com.clinic.common.dto.response.UserResponseDTO;
import com.clinic.common.dto.view.UserDetailViewDTO;
import com.clinic.common.dto.view.UserListViewDTO;
import com.clinic.common.entity.core.User;
import com.clinic.common.security.TenantContext;
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
import java.util.UUID;

/**
 * REST Controller for User management.
 *
 * Implements CQRS pattern:
 * - READ operations: Use database views (v_user_list, v_user_detail) for optimized reads
 * - WRITE operations: Use JPA entities for proper relationship management
 *
 * All operations are tenant-scoped using TenantContext.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    // ========================================================================
    // READ ENDPOINTS (CQRS: using database views)
    // ========================================================================

    /**
     * List all users for the current tenant.
     * Uses v_user_list view for optimized read with pre-joined role information.
     */
    @GetMapping
    @Operation(summary = "List all users", description = "Get all users for the current tenant with aggregated role information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user list"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<UserListViewDTO>> listUsers() {
        UUID tenantId = getTenantId();
        log.debug("Listing users for tenant: {}", tenantId);

        List<UserListViewDTO> users = userService.getUserListView(tenantId);
        return ResponseEntity.ok(users);
    }

    /**
     * Get user details by ID.
     * Uses v_user_detail view for complete user profile with roles and permissions.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user details", description = "Get complete user profile with roles and permissions hierarchy")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user details"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserDetailViewDTO> getUserDetail(
            @Parameter(description = "User ID") @PathVariable UUID id) {
        UUID tenantId = getTenantId();
        log.debug("Getting user detail: {} for tenant: {}", id, tenantId);

        return userService.getUserDetailView(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List doctors only.
     * Uses v_user_list view filtered by DOCTOR role.
     */
    @GetMapping("/doctors")
    @Operation(summary = "List doctors", description = "Get all users with DOCTOR role for the current tenant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved doctors list"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<UserListViewDTO>> listDoctors() {
        UUID tenantId = getTenantId();
        log.debug("Listing doctors for tenant: {}", tenantId);

        List<UserListViewDTO> doctors = userService.getDoctorsListView(tenantId);
        return ResponseEntity.ok(doctors);
    }

    /**
     * Search users by name or email.
     * Uses v_user_list view with ILIKE search.
     */
    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search users by name or email (case-insensitive)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved search results"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<UserListViewDTO>> searchUsers(
            @Parameter(description = "Search term for name or email") @RequestParam("q") String searchTerm) {
        UUID tenantId = getTenantId();
        log.debug("Searching users with term '{}' for tenant: {}", searchTerm, tenantId);

        List<UserListViewDTO> users = userService.searchUsersView(tenantId, searchTerm);
        return ResponseEntity.ok(users);
    }

    // ========================================================================
    // WRITE ENDPOINTS (CQRS: using JPA entities)
    // ========================================================================

    /**
     * Create a new user.
     * Password is hashed with BCrypt before storage.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user", description = "Create a new user (Admin only). Password is automatically hashed.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<UserResponseDTO> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UUID tenantId = getTenantId();
        log.info("Creating user: {} for tenant: {}", request.getEmail(), tenantId);

        // Map request to entity
        User user = userMapper.toEntity(request);
        // Password will be hashed by service
        user.setPasswordHash(request.getPassword());

        User created = userService.createUser(user, tenantId);
        UserResponseDTO response = userMapper.toResponseDTO(created);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing user.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user", description = "Update user details (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<UserResponseDTO> updateUser(
            @Parameter(description = "User ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        UUID tenantId = getTenantId();
        log.info("Updating user: {} for tenant: {}", id, tenantId);

        // Build updates entity from request
        User updates = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .build();

        // Handle status update separately if provided
        User existingUser = userService.getUserById(id, tenantId);
        if (request.getStatus() != null && request.getStatus() != existingUser.getStatus()) {
            userService.updateUserStatus(id, tenantId, request.getStatus());
        }

        User updated = userService.updateUser(id, tenantId, updates);
        UserResponseDTO response = userMapper.toResponseDTO(updated);

        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate (soft delete) a user.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate user", description = "Soft delete a user (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<Void> deactivateUser(
            @Parameter(description = "User ID") @PathVariable UUID id) {
        UUID tenantId = getTenantId();
        log.info("Deactivating user: {} for tenant: {}", id, tenantId);

        userService.softDeleteUser(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Assign roles to a user.
     */
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign roles", description = "Assign roles to a user (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid role IDs"),
            @ApiResponse(responseCode = "404", description = "User or role not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<UserResponseDTO> assignRoles(
            @Parameter(description = "User ID") @PathVariable UUID id,
            @Valid @RequestBody AssignRolesRequest request) {
        UUID tenantId = getTenantId();
        log.info("Assigning {} roles to user: {} for tenant: {}",
                request.getRoleIds().size(), id, tenantId);

        User updated = userService.assignRolesToUser(id, tenantId, request.getRoleIds());
        UserResponseDTO response = userMapper.toResponseDTO(updated);

        return ResponseEntity.ok(response);
    }

    /**
     * Remove a role from a user.
     */
    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove role", description = "Remove a role from a user (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role removed successfully"),
            @ApiResponse(responseCode = "404", description = "User or role not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<UserResponseDTO> removeRole(
            @Parameter(description = "User ID") @PathVariable UUID id,
            @Parameter(description = "Role ID to remove") @PathVariable UUID roleId) {
        UUID tenantId = getTenantId();
        log.info("Removing role: {} from user: {} for tenant: {}", roleId, id, tenantId);

        User updated = userService.removeRoleFromUser(id, tenantId, roleId);
        UserResponseDTO response = userMapper.toResponseDTO(updated);

        return ResponseEntity.ok(response);
    }

    /**
     * Reset user password (admin operation).
     * New password is hashed with BCrypt before storage.
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reset password", description = "Reset user password (Admin only). New password is automatically hashed.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid password format"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<Void> resetPassword(
            @Parameter(description = "User ID") @PathVariable UUID id,
            @Valid @RequestBody ResetPasswordRequest request) {
        UUID tenantId = getTenantId();
        log.info("Resetting password for user: {} in tenant: {}", id, tenantId);

        userService.resetPassword(id, tenantId, request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    /**
     * Unlock a locked user account.
     */
    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unlock account", description = "Unlock a locked user account (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account unlocked successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<Void> unlockAccount(
            @Parameter(description = "User ID") @PathVariable UUID id) {
        UUID tenantId = getTenantId();
        log.info("Unlocking account for user: {} in tenant: {}", id, tenantId);

        userService.unlockAccount(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Get current tenant ID from context.
     * Throws IllegalStateException if tenant context is not set.
     */
    private UUID getTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not set");
        }
        return tenantId;
    }
}
