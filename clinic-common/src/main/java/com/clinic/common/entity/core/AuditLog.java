package com.clinic.common.entity.core;

import com.clinic.common.enums.AuditAction;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Type;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_tenant", columnList = "tenant_id, timestamp"),
    @Index(name = "idx_audit_logs_user", columnList = "user_id, timestamp"),
    @Index(name = "idx_audit_logs_entity", columnList = "entity_type, entity_id, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    @NotNull(message = "Action is required")
    private AuditAction action;

    @Column(name = "entity_type", nullable = false, length = 100)
    @NotBlank(message = "Entity type is required")
    @Size(max = 100)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Type(JsonBinaryType.class)
    @Column(name = "old_values", columnDefinition = "jsonb")
    private Map<String, Object> oldValues;

    @Type(JsonBinaryType.class)
    @Column(name = "new_values", columnDefinition = "jsonb")
    private Map<String, Object> newValues;

    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "timestamp", nullable = false)
    @NotNull
    private Instant timestamp = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
