package com.clinic.backend.repository;

import com.clinic.backend.entity.AuditLog;
import com.clinic.common.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    // Tenant-scoped audit queries (with pagination for large datasets)
    Page<AuditLog> findByTenantId(UUID tenantId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndAction(UUID tenantId, AuditAction action, Pageable pageable);

    Page<AuditLog> findByTenantIdAndUserId(UUID tenantId, UUID userId, Pageable pageable);

    // Entity-based audit trail (immutable sequence)
    Page<AuditLog> findByTenantIdAndEntityTypeAndEntityId(UUID tenantId, String entityType, UUID entityId, Pageable pageable);

    // Time-based queries (Monotonic sequences)
    Page<AuditLog> findByTenantIdAndTimestampBetween(UUID tenantId, Instant start, Instant end, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentActivityByTenant(@Param("tenantId") UUID tenantId, @Param("since") Instant since);

    // Action-based queries
    Page<AuditLog> findByActionAndTenantId(AuditAction action, UUID tenantId, Pageable pageable);

    List<AuditLog> findByActionInAndTenantId(List<AuditAction> actions, UUID tenantId);

    // User activity tracking
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.tenantId = :tenantId AND a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AuditLog> findUserActivitySince(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId, @Param("since") Instant since);

    // IP-based queries (security monitoring)
    @Query("SELECT a FROM AuditLog a WHERE a.ipAddress = :ipAddress AND a.tenantId = :tenantId AND a.timestamp >= :since")
    List<AuditLog> findByIpAddressAndTenantIdSince(@Param("ipAddress") String ipAddress, @Param("tenantId") UUID tenantId, @Param("since") Instant since);

    // Count queries for analytics
    long countByTenantIdAndTimestampBetween(UUID tenantId, Instant start, Instant end);

    long countByTenantIdAndActionAndTimestampBetween(UUID tenantId, AuditAction action, Instant start, Instant end);
}
