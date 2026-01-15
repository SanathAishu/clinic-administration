package com.clinic.common.entity.core;

import com.clinic.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessions", indexes = {
    @Index(name = "idx_sessions_tenant_user", columnList = "tenant_id, user_id"),
    @Index(name = "idx_sessions_token", columnList = "token_jti"),
    @Index(name = "idx_sessions_expires", columnList = "expires_at"),
    @Index(name = "idx_sessions_user_active", columnList = "user_id, expires_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "sessions_token_jti_unique", columnNames = "token_jti"),
    @UniqueConstraint(name = "sessions_refresh_token_jti_unique", columnNames = "refresh_token_jti")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    @Column(name = "token_jti", nullable = false, unique = true)
    @NotBlank(message = "Token JTI is required")
    @Size(max = 255)
    private String tokenJti;

    @Column(name = "refresh_token_jti", unique = true)
    @Size(max = 255)
    private String refreshTokenJti;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    @NotNull
    private Instant expiresAt;

    @Column(name = "refresh_expires_at")
    private Instant refreshExpiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    private User revokedBy;

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }

    public void revoke(User revokedByUser) {
        this.revokedAt = Instant.now();
        this.revokedBy = revokedByUser;
    }
}
