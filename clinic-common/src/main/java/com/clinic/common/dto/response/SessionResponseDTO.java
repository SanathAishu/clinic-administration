package com.clinic.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponseDTO {

    private UUID id;
    private UUID userId;
    private String tokenJti;
    private String ipAddress;
    private String userAgent;
    private Instant issuedAt; // maps to createdAt from entity
    private Instant expiresAt;
    private Instant revokedAt;
    private boolean isActive;

    // Do NOT expose: refreshTokenHash (security-sensitive)
}
