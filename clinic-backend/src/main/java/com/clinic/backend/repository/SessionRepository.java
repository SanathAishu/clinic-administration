package com.clinic.backend.repository;

import com.clinic.backend.entity.Session;
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
public interface SessionRepository extends JpaRepository<Session, UUID> {

    // Token-based lookup (Bijective mapping: token ↔ session)
    Optional<Session> findByTokenJti(String tokenJti);

    Optional<Session> findByRefreshTokenJti(String refreshTokenJti);

    // Valid session lookup
    @Query("SELECT s FROM Session s WHERE s.tokenJti = :jti AND s.revokedAt IS NULL AND s.expiresAt > :now")
    Optional<Session> findValidSessionByJti(@Param("jti") String jti, @Param("now") Instant now);

    @Query("SELECT s FROM Session s WHERE s.refreshTokenJti = :jti AND s.revokedAt IS NULL AND s.refreshExpiresAt > :now")
    Optional<Session> findValidSessionByRefreshJti(@Param("jti") String jti, @Param("now") Instant now);

    // User sessions (tenant-scoped)
    @Query("SELECT s FROM Session s WHERE s.user.id = :userId AND s.user.tenantId = :tenantId AND s.revokedAt IS NULL")
    List<Session> findActiveSessionsByUserAndTenant(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    @Query("SELECT s FROM Session s WHERE s.user.id = :userId AND s.user.tenantId = :tenantId AND s.revokedAt IS NULL AND s.expiresAt > :now")
    List<Session> findValidSessionsByUserAndTenant(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId, @Param("now") Instant now);

    // Expired sessions cleanup
    @Query("SELECT s FROM Session s WHERE s.expiresAt < :now AND s.revokedAt IS NULL")
    List<Session> findExpiredSessions(@Param("now") Instant now);

    @Query("SELECT s FROM Session s WHERE s.refreshExpiresAt < :now AND s.revokedAt IS NULL")
    List<Session> findExpiredRefreshTokens(@Param("now") Instant now);

    // Revoke operations (Idempotent)
    @Modifying
    @Query("UPDATE Session s SET s.revokedAt = :now WHERE s.user.id = :userId AND s.revokedAt IS NULL")
    int revokeAllUserSessions(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Session s SET s.revokedAt = :now WHERE s.tokenJti = :jti AND s.revokedAt IS NULL")
    int revokeSessionByJti(@Param("jti") String jti, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Session s SET s.revokedAt = :now WHERE s.user.tenantId = :tenantId AND s.revokedAt IS NULL")
    int revokeAllTenantSessions(@Param("tenantId") UUID tenantId, @Param("now") Instant now);

    // Session monitoring
    long countByUserIdAndRevokedAtIsNull(UUID userId);

    @Query("SELECT COUNT(s) FROM Session s WHERE s.user.tenantId = :tenantId AND s.revokedAt IS NULL AND s.expiresAt > :now")
    long countActiveSessionsByTenant(@Param("tenantId") UUID tenantId, @Param("now") Instant now);

    // IP-based session tracking
    @Query("SELECT s FROM Session s WHERE s.ipAddress = :ipAddress AND s.user.tenantId = :tenantId AND s.createdAt >= :since")
    List<Session> findSessionsByIpAndTenantSince(@Param("ipAddress") String ipAddress, @Param("tenantId") UUID tenantId, @Param("since") Instant since);
}
