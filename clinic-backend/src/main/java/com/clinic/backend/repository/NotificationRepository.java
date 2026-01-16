package com.clinic.backend.repository;

import com.clinic.common.entity.operational.Notification;
import com.clinic.common.enums.NotificationStatus;
import com.clinic.common.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Tenant-scoped queries
    Optional<Notification> findByIdAndTenantId(UUID id, UUID tenantId);

    // User notifications
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.user.tenantId = :tenantId " +
           "ORDER BY n.scheduledAt DESC")
    Page<Notification> findUserNotifications(@Param("userId") UUID userId,
                                              @Param("tenantId") UUID tenantId,
                                              Pageable pageable);

    List<Notification> findByUserIdAndTenantIdOrderByScheduledAtDesc(UUID userId, UUID tenantId);

    // Status-based queries
    List<Notification> findByUserIdAndTenantIdAndStatus(UUID userId, UUID tenantId, NotificationStatus status);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.user.tenantId = :tenantId AND " +
           "n.status IN :statuses ORDER BY n.scheduledAt DESC")
    List<Notification> findByUserAndStatuses(@Param("userId") UUID userId,
                                              @Param("tenantId") UUID tenantId,
                                              @Param("statuses") List<NotificationStatus> statuses);

    // Unread notifications
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.user.tenantId = :tenantId AND " +
           "n.status != 'READ' ORDER BY n.scheduledAt DESC")
    List<Notification> findUnreadNotifications(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    long countByUserIdAndTenantIdAndStatusNot(UUID userId, UUID tenantId, NotificationStatus status);

    // Type-based queries
    List<Notification> findByUserIdAndTenantIdAndType(UUID userId, UUID tenantId, NotificationType type);

    // Scheduled notifications (for sending)
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.scheduledAt <= :now " +
           "ORDER BY n.scheduledAt ASC")
    List<Notification> findScheduledNotificationsToSend(@Param("now") Instant now);

    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.status = 'PENDING' AND " +
           "n.scheduledAt <= :now ORDER BY n.scheduledAt ASC")
    List<Notification> findScheduledNotificationsForTenant(@Param("tenantId") UUID tenantId, @Param("now") Instant now);

    // Failed notifications (for retry)
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.tenantId = :tenantId " +
           "ORDER BY n.scheduledAt ASC")
    List<Notification> findFailedNotifications(@Param("tenantId") UUID tenantId);

    // Reference-based queries
    @Query("SELECT n FROM Notification n WHERE n.referenceType = :referenceType AND n.referenceId = :referenceId AND " +
           "n.tenantId = :tenantId ORDER BY n.scheduledAt DESC")
    List<Notification> findByReference(@Param("referenceType") String referenceType,
                                        @Param("referenceId") UUID referenceId,
                                        @Param("tenantId") UUID tenantId);

    // Mark as read (Idempotent operation)
    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :now WHERE n.id = :notificationId AND n.status != 'READ'")
    int markAsRead(@Param("notificationId") UUID notificationId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :now WHERE n.user.id = :userId AND " +
           "n.user.tenantId = :tenantId AND n.status != 'READ'")
    int markAllAsReadForUser(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId, @Param("now") Instant now);

    // Recent notifications
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.user.tenantId = :tenantId AND " +
           "n.scheduledAt >= :since ORDER BY n.scheduledAt DESC")
    List<Notification> findRecentNotifications(@Param("userId") UUID userId,
                                                @Param("tenantId") UUID tenantId,
                                                @Param("since") Instant since);

    // Counting
    long countByUserIdAndTenantIdAndStatus(UUID userId, UUID tenantId, NotificationStatus status);

    long countByTenantIdAndStatusAndScheduledAtBetween(UUID tenantId, NotificationStatus status, Instant start, Instant end);

    // Additional methods for service
    @Query("SELECT n FROM Notification n WHERE n.id = :id AND n.tenantId = :tenantId")
    Optional<Notification> findByIdAndTenantIdAndDeletedAtIsNull(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.tenantId = :tenantId " +
           "ORDER BY n.scheduledAt DESC")
    Page<Notification> findByUserIdAndTenantIdAndDeletedAtIsNull(@Param("userId") UUID userId,
                                                                  @Param("tenantId") UUID tenantId,
                                                                  Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.tenantId = :tenantId AND " +
           "n.readAt IS NULL ORDER BY n.scheduledAt DESC")
    List<Notification> findUnreadNotificationsForUser(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.type = :notificationType " +
           "ORDER BY n.scheduledAt DESC")
    List<Notification> findByTenantIdAndNotificationType(@Param("tenantId") UUID tenantId,
                                                          @Param("notificationType") NotificationType notificationType);

    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.status = 'PENDING' " +
           "ORDER BY n.scheduledAt ASC")
    List<Notification> findPendingNotifications(@Param("tenantId") UUID tenantId);

    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.status = 'PENDING' AND " +
           "n.scheduledAt <= :scheduledBefore ORDER BY n.scheduledAt ASC")
    List<Notification> findScheduledNotifications(@Param("tenantId") UUID tenantId, @Param("scheduledBefore") Instant scheduledBefore);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.tenantId = :tenantId AND " +
           "n.readAt IS NULL")
    long countUnreadNotificationsForUser(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);
}
