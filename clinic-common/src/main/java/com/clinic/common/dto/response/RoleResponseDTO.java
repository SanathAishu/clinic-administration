package com.clinic.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponseDTO {

    private UUID id;
    private String name;
    private String description;
    private Boolean isSystemRole;
    private Set<PermissionResponseDTO> permissions;
    private Instant createdAt;
    private Instant updatedAt;
}
