package com.clinic.backend.entity;

import com.clinic.common.entity.TenantAwareEntity;
import com.clinic.common.enums.NotificationStatus;
import com.clinic.common.enums.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_tenant", columnList = "tenant_id"),
    @Index(name = "idx_notifications_user_status", columnList = "user_id, status, scheduled_at"),
    @Index(name = "idx_notifications_scheduled", columnList = "scheduled_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    // Notification Details
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @NotNull
    private NotificationType type;

    @Column(name = "title", nullable = false)
    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Message is required")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private NotificationStatus status = NotificationStatus.PENDING;

    // Reference
    @Column(name = "reference_type", length = 100)
    @Size(max = 100)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    // Delivery
    @Column(name = "scheduled_at", nullable = false)
    @NotNull
    private Instant scheduledAt = Instant.now();

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "read_at")
    private Instant readAt;

    // Helper methods
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markAsRead() {
        this.status = NotificationStatus.READ;
        this.readAt = Instant.now();
    }

    public void markAsFailed() {
        this.status = NotificationStatus.FAILED;
    }

    public boolean isPending() {
        return status == NotificationStatus.PENDING;
    }

    public boolean isRead() {
        return status == NotificationStatus.READ;
    }
}
