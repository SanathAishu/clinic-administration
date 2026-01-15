package com.clinic.common.dto.view;

import com.clinic.common.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO mapping to v_user_detail database view.
 * Used for detailed user view with roles and permissions hierarchy.
 *
 * CQRS Pattern: This DTO is used for READ operations via native queries.
 * WRITE operations use the User entity directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailViewDTO {

    private UUID id;
    private UUID tenantId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private UserStatus status;
    private Instant lastLoginAt;
    private Integer loginAttempts;
    private Instant lockedUntil;
    private Instant passwordChangedAt;

    // Tenant info
    private String tenantName;
    private String tenantSubdomain;

    // Aggregated roles with nested permissions as JSON string
    private String rolesWithPermissions;

    // Flat list of permission names as JSON array string
    private String permissionNames;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
