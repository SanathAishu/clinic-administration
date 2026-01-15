package com.clinic.backend.service;

import com.clinic.common.entity.operational.Notification;
import com.clinic.backend.repository.NotificationRepository;
import com.clinic.common.enums.NotificationStatus;
import com.clinic.common.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification createNotification(Notification notification, UUID tenantId) {
        log.debug("Creating notification for user: {}", notification.getUser().getId());

        notification.setTenantId(tenantId);

        if (notification.getStatus() == null) {
            notification.setStatus(NotificationStatus.PENDING);
        }

        if (notification.getScheduledAt() == null) {
            notification.setScheduledAt(Instant.now());
        }

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification: {} (Type: {})", saved.getId(), saved.getType());
        return saved;
    }

    public Notification getNotificationById(UUID id, UUID tenantId) {
        return notificationRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + id));
    }

    public Page<Notification> getNotificationsForUser(UUID userId, UUID tenantId, Pageable pageable) {
        return notificationRepository.findByUserIdAndTenantIdAndDeletedAtIsNull(userId, tenantId, pageable);
    }

    public List<Notification> getUnreadNotificationsForUser(UUID userId, UUID tenantId) {
        return notificationRepository.findUnreadNotificationsForUser(userId, tenantId);
    }

    public List<Notification> getNotificationsByType(UUID tenantId, NotificationType notificationType) {
        return notificationRepository.findByTenantIdAndNotificationType(tenantId, notificationType);
    }

    public List<Notification> getPendingNotifications(UUID tenantId) {
        return notificationRepository.findPendingNotifications(tenantId);
    }

    public List<Notification> getScheduledNotifications(UUID tenantId, Instant scheduledBefore) {
        return notificationRepository.findScheduledNotifications(tenantId, scheduledBefore);
    }

    @Transactional
    public Notification markAsRead(UUID id, UUID tenantId) {
        Notification notification = getNotificationById(id, tenantId);
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(Instant.now());
        Notification saved = notificationRepository.save(notification);
        log.debug("Marked notification as read: {}", saved.getId());
        return saved;
    }

    @Transactional
    public void markAllAsReadForUser(UUID userId, UUID tenantId) {
        List<Notification> unread = getUnreadNotificationsForUser(userId, tenantId);
        Instant now = Instant.now();

        for (Notification notification : unread) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(now);
        }

        notificationRepository.saveAll(unread);
        log.info("Marked {} notifications as read for user: {}", unread.size(), userId);
    }

    @Transactional
    public Notification sendNotification(UUID id, UUID tenantId) {
        Notification notification = getNotificationById(id, tenantId);

        if (notification.getStatus() != NotificationStatus.PENDING) {
            throw new IllegalStateException("Can only send pending notifications");
        }

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(Instant.now());
        Notification saved = notificationRepository.save(notification);
        log.info("Sent notification: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Notification markAsDelivered(UUID id, UUID tenantId) {
        Notification notification = getNotificationById(id, tenantId);
        // Note: Notification entity doesn't have DELIVERED status, using SENT instead
        notification.setStatus(NotificationStatus.SENT);
        Notification saved = notificationRepository.save(notification);
        log.info("Marked notification as delivered/sent: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Notification markAsFailed(UUID id, UUID tenantId, String errorMessage) {
        Notification notification = getNotificationById(id, tenantId);
        notification.setStatus(NotificationStatus.FAILED);
        // Note: Notification entity doesn't have notes field
        // Error details should be stored in a separate audit/error log
        Notification saved = notificationRepository.save(notification);
        log.warn("Marked notification as failed: {} - {}", saved.getId(), errorMessage);
        return saved;
    }

    @Transactional
    public Notification updateNotification(UUID id, UUID tenantId, Notification updates) {
        Notification notification = getNotificationById(id, tenantId);

        if (updates.getTitle() != null) notification.setTitle(updates.getTitle());
        if (updates.getMessage() != null) notification.setMessage(updates.getMessage());
        if (updates.getType() != null) notification.setType(updates.getType());
        if (updates.getScheduledAt() != null) notification.setScheduledAt(updates.getScheduledAt());

        return notificationRepository.save(notification);
    }

    @Transactional
    public void softDeleteNotification(UUID id, UUID tenantId) {
        Notification notification = getNotificationById(id, tenantId);
        // Note: Notification doesn't extend SoftDeletableEntity, so we hard delete
        // TODO: Consider changing Notification to extend SoftDeletableEntity
        notificationRepository.delete(notification);
        log.info("Deleted notification: {}", id);
    }

    public long countUnreadNotificationsForUser(UUID userId, UUID tenantId) {
        return notificationRepository.countUnreadNotificationsForUser(userId, tenantId);
    }
}
