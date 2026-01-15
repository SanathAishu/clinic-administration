package com.clinic.backend.security;

import com.clinic.backend.service.SessionService;
import com.clinic.common.security.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * JWT Authentication Filter that processes each request to validate JWT tokens.
 *
 * This filter:
 * 1. Extracts JWT from Authorization header
 * 2. Validates the JWT signature and expiration
 * 3. Validates the session is not revoked (database check)
 * 4. Sets TenantContext for RLS (Row Level Security)
 * 5. Sets SecurityContext with authenticated user
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final SessionService sessionService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // 1. Extract claims from token
                UUID userId = tokenProvider.getUserIdFromToken(jwt);
                UUID tenantId = tokenProvider.getTenantIdFromToken(jwt);
                String tokenJti = tokenProvider.getJtiFromToken(jwt);

                // 2. Validate session is not revoked (prevents use of invalidated tokens)
                if (!sessionService.isSessionValid(tokenJti)) {
                    log.warn("Token JTI {} is no longer valid (session revoked or expired)", tokenJti);
                    clearContextAndContinue(request, response, filterChain);
                    return;
                }

                // 3. Set tenant context for RLS before loading user
                TenantContext.setCurrentTenant(tenantId);

                // 4. Load user details (includes roles and permissions)
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId.toString());

                // 5. Validate user is still active
                if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                    log.warn("User {} is disabled or locked", userId);
                    clearContextAndContinue(request, response, filterChain);
                    return;
                }

                // 6. Create authentication token and set in SecurityContext
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Set authentication for user: {} in tenant: {}", userId, tenantId);
            }

            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            log.error("Could not set user authentication in security context: {}", ex.getMessage());
            // Clear any partial context on error
            TenantContext.clear();
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
        } finally {
            // CRITICAL: Always clear TenantContext after request to prevent thread-local leaks
            TenantContext.clear();
        }
    }

    /**
     * Clear security context and tenant context, then continue filter chain.
     * Used when token validation fails but we want to continue (for public endpoints).
     */
    private void clearContextAndContinue(HttpServletRequest request, HttpServletResponse response,
                                          FilterChain filterChain) throws IOException, ServletException {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header.
     * Expects format: "Bearer <token>"
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    /**
     * Skip filter for public endpoints (optimization).
     * Spring Security will handle the authorization check.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip filter for public auth endpoints except /me and /validate which need auth
        return path.equals("/api/auth/login") ||
               path.equals("/api/auth/refresh") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/api/docs/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/");
    }
}
