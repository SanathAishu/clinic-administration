package com.clinic.common.entity.clinical;

import com.clinic.common.entity.core.User;
import com.clinic.common.entity.TenantAwareEntity;
import com.clinic.common.entity.patient.Patient;
import com.clinic.common.enums.ConsentStatus;
import com.clinic.common.enums.ConsentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consent_records", indexes = {
    @Index(name = "idx_consent_records_tenant", columnList = "tenant_id"),
    @Index(name = "idx_consent_records_patient", columnList = "patient_id, granted_at"),
    @Index(name = "idx_consent_records_status", columnList = "status"),
    @Index(name = "idx_consent_records_expiry", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentRecord extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @NotNull
    private Patient patient;

    // Consent Details
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false)
    @NotNull
    private ConsentType consentType;

    @Column(name = "purpose", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Purpose is required")
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private ConsentStatus status = ConsentStatus.GRANTED;

    // Legal Requirements
    @Column(name = "consent_text", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Consent text is required")
    private String consentText;

    @Column(name = "consent_version", nullable = false, length = 50)
    @NotBlank(message = "Consent version is required")
    @Size(max = 50)
    private String consentVersion;

    // Timestamps
    @Column(name = "granted_at", nullable = false)
    @NotNull
    private Instant grantedAt = Instant.now();

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    // Digital Signature/Proof
    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "signature_data")
    private byte[] signatureData;

    // Metadata
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by")
    private User grantedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    private User revokedBy;

    // Helper methods
    public void revoke(User user) {
        this.status = ConsentStatus.REVOKED;
        this.revokedAt = Instant.now();
        this.revokedBy = user;
    }

    public boolean isActive() {
        if (status != ConsentStatus.GRANTED) {
            return false;
        }
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            return false;
        }
        return true;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    @PreUpdate
    protected void checkExpiry() {
        if (isExpired() && status == ConsentStatus.GRANTED) {
            this.status = ConsentStatus.EXPIRED;
        }
    }
}
