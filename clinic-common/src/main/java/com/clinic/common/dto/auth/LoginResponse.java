package com.clinic.common.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresIn;

    // User information
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private UUID tenantId;
    private String tenantName;

    // Permissions
    private List<String> roles;
    private List<String> permissions;
}
