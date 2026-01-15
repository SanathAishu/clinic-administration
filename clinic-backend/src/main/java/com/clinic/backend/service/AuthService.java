package com.clinic.backend.service;

import com.clinic.backend.repository.UserRepository;
import com.clinic.backend.security.CustomUserDetailsService;
import com.clinic.backend.security.JwtTokenProvider;
import com.clinic.backend.security.UserPrincipal;
import com.clinic.common.dto.auth.LoginRequest;
import com.clinic.common.dto.auth.LoginResponse;
import com.clinic.common.dto.auth.RefreshTokenRequest;
import com.clinic.common.entity.core.Session;
import com.clinic.common.entity.core.Tenant;
import com.clinic.common.entity.core.User;
import com.clinic.common.enums.TenantStatus;
import com.clinic.common.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Authentication service handling login, logout, and token refresh operations.
 * Implements security best practices including session tracking for token invalidation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final UserService userService;
    private final TenantService tenantService;
    private final SessionService sessionService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticate user and generate JWT tokens.
     * Creates a session record for token tracking and invalidation support.
     *
     * @param loginRequest Login credentials including email, password, and tenant subdomain
     * @param ipAddress Client IP address for session tracking
     * @param userAgent Client user agent for session tracking
     * @return LoginResponse containing access token, refresh token, and user info
     */
    @Transactional
    public LoginResponse login(LoginRequest loginRequest, String ipAddress, String userAgent) {
        log.debug("Login attempt for email: {} in tenant: {}", loginRequest.getEmail(), loginRequest.getTenantSubdomain());

        // 1. Resolve tenant by subdomain (Injective lookup)
        Tenant tenant = tenantService.getTenantBySubdomain(loginRequest.getTenantSubdomain());
        validateTenantStatus(tenant);

        // 2. Find user in tenant scope (Injective function: (email, tenantId) -> user)
        User user = userRepository.findByEmailAndTenantIdForAuthentication(
                        loginRequest.getEmail(), tenant.getId())
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found - {}", loginRequest.getEmail());
                    return new BadCredentialsException("Invalid email or password");
                });

        // 3. Validate user status (State machine check)
        validateUserStatus(user);

        // 4. Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user, tenant.getId());
            throw new BadCredentialsException("Invalid email or password");
        }

        // 5. Load user principal with roles and permissions
        UserPrincipal userPrincipal = (UserPrincipal) userDetailsService.loadUserByEmailAndTenant(
                loginRequest.getEmail(), tenant.getId());

        // 6. Generate tokens
        JwtTokenProvider.TokenResult accessTokenResult = tokenProvider.generateAccessToken(
                userPrincipal, tenant.getId(), userPrincipal.getRoles());
        JwtTokenProvider.TokenResult refreshTokenResult = tokenProvider.generateRefreshToken(
                user.getId(), tenant.getId());

        // 7. Create session record for token tracking (Bijective mapping: tokenJti <-> session)
        Session session = createSession(user, accessTokenResult, refreshTokenResult, ipAddress, userAgent);

        // 8. Handle successful login (reset failed attempts, update last login)
        userService.handleSuccessfulLogin(user.getId(), tenant.getId());

        log.info("User logged in successfully: {} (tenant: {})", user.getEmail(), tenant.getSubdomain());

        // 9. Build and return response
        return buildLoginResponse(userPrincipal, tenant, accessTokenResult, refreshTokenResult);
    }

    /**
     * Refresh access token using a valid refresh token.
     * Rotates both access and refresh tokens, updates session record.
     *
     * @param refreshRequest Contains the refresh token
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     * @return New LoginResponse with rotated tokens
     */
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest refreshRequest, String ipAddress, String userAgent) {
        String refreshToken = refreshRequest.getRefreshToken();

        // 1. Validate refresh token
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        // 2. Verify it's actually a refresh token
        if (!tokenProvider.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Token is not a refresh token");
        }

        // 3. Extract claims from refresh token
        String refreshJti = tokenProvider.getJtiFromToken(refreshToken);
        UUID userId = tokenProvider.getUserIdFromToken(refreshToken);
        UUID tenantId = tokenProvider.getTenantIdFromToken(refreshToken);

        // 4. Validate session exists and is not revoked (Session validity check)
        Session session = sessionService.getValidSessionByRefreshTokenJti(refreshJti);

        // 5. Load user and validate status
        User user = userService.getUserById(userId, tenantId);
        validateUserStatus(user);

        // 6. Validate tenant status
        Tenant tenant = tenantService.getTenantById(tenantId);
        validateTenantStatus(tenant);

        // 7. Load user principal with roles
        UserPrincipal userPrincipal = (UserPrincipal) userDetailsService.loadUserById(userId);

        // 8. Generate new tokens (token rotation)
        JwtTokenProvider.TokenResult newAccessToken = tokenProvider.generateAccessToken(
                userPrincipal, tenantId, userPrincipal.getRoles());
        JwtTokenProvider.TokenResult newRefreshToken = tokenProvider.generateRefreshToken(userId, tenantId);

        // 9. Update session with new tokens (maintains session continuity)
        sessionService.refreshSession(
                refreshJti,
                newAccessToken.jti(),
                newRefreshToken.jti(),
                newAccessToken.expiresAt(),
                newRefreshToken.expiresAt()
        );

        log.info("Token refreshed for user: {} (tenant: {})", user.getEmail(), tenant.getSubdomain());

        return buildLoginResponse(userPrincipal, tenant, newAccessToken, newRefreshToken);
    }

    /**
     * Logout user by invalidating the current session.
     * Revokes the session associated with the provided token JTI.
     *
     * @param tokenJti JWT ID of the current access token
     */
    @Transactional
    public void logout(String tokenJti) {
        log.debug("Logout request for token JTI: {}", tokenJti);
        sessionService.revokeSession(tokenJti);
        SecurityContextHolder.clearContext();
        log.info("User logged out successfully");
    }

    /**
     * Logout user from all devices by revoking all sessions.
     *
     * @param userId User ID to logout
     */
    @Transactional
    public void logoutAllDevices(UUID userId) {
        log.debug("Logout all devices request for user: {}", userId);
        int revokedCount = sessionService.revokeAllUserSessions(userId);
        SecurityContextHolder.clearContext();
        log.info("Revoked {} sessions for user: {}", revokedCount, userId);
    }

    /**
     * Get current authenticated user information.
     *
     * @return LoginResponse with user info (without new tokens)
     */
    @Transactional(readOnly = true)
    public LoginResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("No authenticated user found");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Tenant tenant = tenantService.getTenantById(userPrincipal.getTenantId());

        return LoginResponse.builder()
                .userId(userPrincipal.getId())
                .email(userPrincipal.getEmail())
                .firstName(userPrincipal.getFirstName())
                .lastName(userPrincipal.getLastName())
                .tenantId(tenant.getId())
                .tenantName(tenant.getName())
                .roles(userPrincipal.getRoles())
                .permissions(userPrincipal.getPermissions())
                .build();
    }

    /**
     * Validate session is still active (not revoked or expired).
     * Called by JWT filter to check token validity against session store.
     *
     * @param tokenJti JWT ID to validate
     * @return true if session is valid, false otherwise
     */
    public boolean isSessionValid(String tokenJti) {
        return sessionService.isSessionValid(tokenJti);
    }

    // ============ Private Helper Methods ============

    /**
     * Validate tenant is in a status that allows login
     */
    private void validateTenantStatus(Tenant tenant) {
        if (tenant.getStatus() == TenantStatus.SUSPENDED) {
            log.warn("Login attempt for suspended tenant: {}", tenant.getSubdomain());
            throw new BadCredentialsException("Tenant account is suspended. Please contact support.");
        }
        if (tenant.getStatus() == TenantStatus.EXPIRED) {
            log.warn("Login attempt for expired tenant: {}", tenant.getSubdomain());
            throw new BadCredentialsException("Tenant subscription has expired. Please renew.");
        }
    }

    /**
     * Validate user is in a status that allows login (State machine validation)
     */
    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.LOCKED) {
            log.warn("Login attempt for locked user: {}", user.getEmail());
            throw new LockedException("Account is locked. Please contact administrator.");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            log.warn("Login attempt for suspended user: {}", user.getEmail());
            throw new LockedException("Account is suspended. Please contact administrator.");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("Login attempt for inactive user: {}", user.getEmail());
            throw new BadCredentialsException("Account is inactive. Please contact administrator.");
        }
        if (user.isLocked()) {
            log.warn("Login attempt for temporarily locked user: {}", user.getEmail());
            throw new LockedException("Account is temporarily locked due to too many failed login attempts.");
        }
    }

    /**
     * Handle failed login attempt - increment counter and potentially lock account
     */
    private void handleFailedLogin(User user, UUID tenantId) {
        log.warn("Failed login attempt for user: {}", user.getEmail());
        userService.handleFailedLogin(user.getId(), tenantId);
    }

    /**
     * Create session record for token tracking
     */
    private Session createSession(User user, JwtTokenProvider.TokenResult accessToken,
                                   JwtTokenProvider.TokenResult refreshToken,
                                   String ipAddress, String userAgent) {
        Session session = Session.builder()
                .user(user)
                .tokenJti(accessToken.jti())
                .refreshTokenJti(refreshToken.jti())
                .expiresAt(accessToken.expiresAt())
                .refreshExpiresAt(refreshToken.expiresAt())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        session.setTenantId(user.getTenantId());

        return sessionService.createSession(session);
    }

    /**
     * Build LoginResponse from components
     */
    private LoginResponse buildLoginResponse(UserPrincipal userPrincipal, Tenant tenant,
                                              JwtTokenProvider.TokenResult accessToken,
                                              JwtTokenProvider.TokenResult refreshToken) {
        return LoginResponse.builder()
                .accessToken(accessToken.token())
                .refreshToken(refreshToken.token())
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpirationMs() / 1000) // Convert to seconds
                .userId(userPrincipal.getId())
                .email(userPrincipal.getEmail())
                .firstName(userPrincipal.getFirstName())
                .lastName(userPrincipal.getLastName())
                .tenantId(tenant.getId())
                .tenantName(tenant.getName())
                .roles(userPrincipal.getRoles())
                .permissions(userPrincipal.getPermissions())
                .build();
    }
}
