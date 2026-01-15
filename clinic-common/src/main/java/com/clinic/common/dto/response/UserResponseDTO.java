package com.clinic.common.dto.response;

import com.clinic.common.enums.UserStatus;
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
public class UserResponseDTO {

    private UUID id;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String fullName;
    private UserStatus status;
    private Integer loginAttempts;
    private Instant lockedUntil;
    private Instant lastLoginAt;
    private boolean isLocked;
    private Set<RoleResponseDTO> roles;
    private Instant createdAt;
    private Instant updatedAt;

    // Do NOT expose: passwordHash, passwordChangedAt, tenantId (internal)
}
