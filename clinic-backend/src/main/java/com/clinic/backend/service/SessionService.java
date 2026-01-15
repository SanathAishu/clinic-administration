package com.clinic.backend.service;

import com.clinic.common.entity.core.Session;
import com.clinic.common.entity.core.User;
import com.clinic.backend.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private final SessionRepository sessionRepository;

    /**
     * Create new session (Bijective mapping: tokenJti ↔ session)
     */
    @Transactional
    public Session createSession(Session session) {
        log.debug("Creating session for user: {} with JTI: {}",
                session.getUser().getId(), session.getTokenJti());

        // Uniqueness validation (Bijective function)
        if (sessionRepository.findByTokenJti(session.getTokenJti()).isPresent()) {
            throw new IllegalArgumentException("Session with this token JTI already exists");
        }

        if (sessionRepository.findByRefreshTokenJti(session.getRefreshTokenJti()).isPresent()) {
            throw new IllegalArgumentException("Session with this refresh token JTI already exists");
        }

        Session saved = sessionRepository.save(session);
        log.info("Created session: {} for user: {}", saved.getId(), saved.getUser().getId());
        return saved;
    }

    /**
     * Get session by token JTI (Bijective lookup)
     */
    public Session getSessionByTokenJti(String tokenJti) {
        return sessionRepository.findByTokenJti(tokenJti)
                .orElseThrow(() -> new IllegalArgumentException("Session not found for token JTI: " + tokenJti));
    }

    /**
     * Get session by refresh token JTI
     */
    public Session getSessionByRefreshTokenJti(String refreshTokenJti) {
        return sessionRepository.findByRefreshTokenJti(refreshTokenJti)
                .orElseThrow(() -> new IllegalArgumentException("Session not found for refresh token JTI: " + refreshTokenJti));
    }

    /**
     * Get valid session by token JTI (Boolean Logic: valid = NOT revoked AND NOT expired)
     */
    public Session getValidSessionByTokenJti(String tokenJti) {
        return sessionRepository.findValidSessionByJti(tokenJti, Instant.now())
                .orElseThrow(() -> new IllegalArgumentException("Valid session not found or expired"));
    }

    /**
     * Get valid session by refresh token JTI
     */
    public Session getValidSessionByRefreshTokenJti(String refreshTokenJti) {
        return sessionRepository.findValidSessionByRefreshJti(refreshTokenJti, Instant.now())
                .orElseThrow(() -> new IllegalArgumentException("Valid session not found or expired"));
    }

    /**
     * Get active sessions for user
     */
    public List<Session> getActiveSessionsForUser(UUID userId, UUID tenantId) {
        return sessionRepository.findActiveSessionsByUserAndTenant(userId, tenantId);
    }

    /**
     * Get valid (active and not expired) sessions for user
     */
    public List<Session> getValidSessionsForUser(UUID userId, UUID tenantId) {
        return sessionRepository.findValidSessionsByUserAndTenant(userId, tenantId, Instant.now());
    }

    /**
     * Validate session (Boolean Logic)
     */
    public boolean isSessionValid(String tokenJti) {
        return sessionRepository.findValidSessionByJti(tokenJti, Instant.now()).isPresent();
    }

    /**
     * Revoke session by JTI (Idempotent operation)
     */
    @Transactional
    public void revokeSession(String tokenJti) {
        int updated = sessionRepository.revokeSessionByJti(tokenJti, Instant.now());

        if (updated > 0) {
            log.info("Revoked session with JTI: {}", tokenJti);
        } else {
            log.debug("Session already revoked or not found: {}", tokenJti);
        }
    }

    /**
     * Revoke all sessions for user (Idempotent - for user logout from all devices)
     */
    @Transactional
    public int revokeAllUserSessions(UUID userId) {
        int count = sessionRepository.revokeAllUserSessions(userId, Instant.now());
        log.info("Revoked {} sessions for user: {}", count, userId);
        return count;
    }

    /**
     * Revoke all sessions for tenant (Admin operation - for security incidents)
     */
    @Transactional
    public int revokeAllTenantSessions(UUID tenantId) {
        int count = sessionRepository.revokeAllTenantSessions(tenantId, Instant.now());
        log.warn("Revoked {} sessions for tenant: {}", count, tenantId);
        return count;
    }

    /**
     * Refresh session with new tokens
     */
    @Transactional
    public Session refreshSession(String refreshTokenJti, String newTokenJti,
                                   String newRefreshTokenJti, Instant newExpiry,
                                   Instant newRefreshExpiry) {

        Session session = getValidSessionByRefreshTokenJti(refreshTokenJti);

        // Update with new token information
        session.setTokenJti(newTokenJti);
        session.setRefreshTokenJti(newRefreshTokenJti);
        session.setExpiresAt(newExpiry);
        session.setRefreshExpiresAt(newRefreshExpiry);

        Session saved = sessionRepository.save(session);
        log.info("Refreshed session: {} with new tokens", saved.getId());
        return saved;
    }

    /**
     * Cleanup expired sessions (Scheduled task)
     */
    @Transactional
    public int cleanupExpiredSessions() {
        List<Session> expiredSessions = sessionRepository.findExpiredSessions(Instant.now());

        for (Session session : expiredSessions) {
            if (session.getRevokedAt() == null) {
                session.setRevokedAt(Instant.now());
            }
        }

        if (!expiredSessions.isEmpty()) {
            sessionRepository.saveAll(expiredSessions);
        }

        log.info("Cleaned up {} expired sessions", expiredSessions.size());
        return expiredSessions.size();
    }

    /**
     * Cleanup expired refresh tokens
     */
    @Transactional
    public int cleanupExpiredRefreshTokens() {
        List<Session> expiredRefreshTokens = sessionRepository.findExpiredRefreshTokens(Instant.now());

        for (Session session : expiredRefreshTokens) {
            if (session.getRevokedAt() == null) {
                session.setRevokedAt(Instant.now());
            }
        }

        if (!expiredRefreshTokens.isEmpty()) {
            sessionRepository.saveAll(expiredRefreshTokens);
        }

        log.info("Cleaned up {} expired refresh tokens", expiredRefreshTokens.size());
        return expiredRefreshTokens.size();
    }

    /**
     * Get session count for user
     */
    public long getActiveSessionCountForUser(UUID userId) {
        return sessionRepository.countByUserIdAndRevokedAtIsNull(userId);
    }

    /**
     * Get active session count for tenant
     */
    public long getActiveSessionCountForTenant(UUID tenantId) {
        return sessionRepository.countActiveSessionsByTenant(tenantId, Instant.now());
    }

    /**
     * Get sessions by IP address (Security monitoring)
     */
    public List<Session> getSessionsByIpAddress(String ipAddress, UUID tenantId, Instant since) {
        return sessionRepository.findSessionsByIpAndTenantSince(ipAddress, tenantId, since);
    }

    /**
     * Detect suspicious activity (multiple sessions from different IPs)
     */
    public boolean detectSuspiciousActivity(UUID userId, UUID tenantId) {
        List<Session> activeSessions = getValidSessionsForUser(userId, tenantId);

        if (activeSessions.size() < 2) {
            return false;
        }

        // Check if sessions are from different IP addresses
        long distinctIpCount = activeSessions.stream()
                .map(Session::getIpAddress)
                .filter(ip -> ip != null && !ip.isEmpty())
                .distinct()
                .count();

        // Suspicious if more than 3 different IPs for same user
        return distinctIpCount > 3;
    }
}
