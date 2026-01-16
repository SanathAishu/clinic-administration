package com.clinic.backend.dto.compliance;

import com.clinic.common.enums.AccessType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Sensitive Data Access Log
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessAuditDTO {
    private UUID id;
    private UUID userId;
    private UUID patientId;
    private String entityType;
    private UUID entityId;
    private AccessType accessType;
    private Instant accessTimestamp;
    private String ipAddress;
    private String userAgent;
    private String accessReason;
    private Boolean dataExported;
    private Instant createdAt;
}
