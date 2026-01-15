package com.clinic.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponseDTO {

    private UUID id;
    private UUID userId;
    private String action;
    private String entityType;
    private UUID entityId;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private Instant timestamp;

    // Note: AuditLog is append-only, no create/update DTOs needed
    // AuditLog is created internally by the system, not via API requests
}
