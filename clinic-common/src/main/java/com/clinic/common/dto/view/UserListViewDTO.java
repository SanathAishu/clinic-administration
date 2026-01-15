package com.clinic.common.dto.view;

import com.clinic.common.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO mapping to v_user_list database view.
 * Used for listing users with aggregated role information.
 *
 * CQRS Pattern: This DTO is used for READ operations via native queries.
 * WRITE operations use the User entity directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListViewDTO {

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

    // Aggregated role info as comma-separated string
    private String roleNames;

    // Aggregated role info as JSON string
    private String roles;

    private Integer roleCount;
    private Boolean isActive;
    private Boolean isLocked;
    private Boolean isDeleted;
    private Instant createdAt;
    private Instant updatedAt;
}
