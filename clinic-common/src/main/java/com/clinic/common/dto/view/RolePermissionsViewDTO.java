package com.clinic.common.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO mapping to v_role_permissions database view.
 * Used for listing roles with their permissions and usage statistics.
 *
 * CQRS Pattern: This DTO is used for READ operations via native queries.
 * WRITE operations use the Role entity directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionsViewDTO {

    private UUID id;
    private UUID tenantId;
    private String name;
    private String description;
    private Boolean isSystemRole;

    // Aggregated permissions as JSON string
    private String permissions;

    private Integer permissionCount;
    private Integer userCount;
    private Instant createdAt;
    private Instant updatedAt;
}
