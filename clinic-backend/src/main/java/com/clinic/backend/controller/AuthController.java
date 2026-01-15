package com.clinic.backend.controller;

import com.clinic.backend.security.JwtTokenProvider;
import com.clinic.backend.service.AuthService;
import com.clinic.common.dto.auth.LoginRequest;
import com.clinic.common.dto.auth.LoginResponse;
import com.clinic.common.dto.auth.RefreshTokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for authentication endpoints.
 * Handles login, logout, token refresh, and current user information.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;

    /**
     * Authenticate user and return JWT tokens.
     *
     * POST /api/auth/login
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user with email and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Account locked or suspended"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        log.debug("Login request from IP: {}", ipAddress);

        LoginResponse response = authService.login(loginRequest, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token using refresh token.
     *
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh Token", description = "Get new access token using refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<LoginResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshRequest,
            HttpServletRequest request) {

        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        log.debug("Token refresh request from IP: {}", ipAddress);

        LoginResponse response = authService.refreshToken(refreshRequest, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout current user (invalidate current session).
     *
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidate current session and token")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully logged out"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String jwt = getJwtFromRequest(request);

        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            String tokenJti = tokenProvider.getJtiFromToken(jwt);
            authService.logout(tokenJti);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Successfully logged out");
        return ResponseEntity.ok(response);
    }

    /**
     * Logout from all devices (invalidate all sessions).
     *
     * POST /api/auth/logout-all
     */
    @PostMapping("/logout-all")
    @Operation(summary = "Logout All Devices", description = "Invalidate all sessions for current user")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully logged out from all devices"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, String>> logoutAllDevices() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        com.clinic.backend.security.UserPrincipal userPrincipal =
                (com.clinic.backend.security.UserPrincipal) authentication.getPrincipal();

        authService.logoutAllDevices(userPrincipal.getId());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Successfully logged out from all devices");
        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user information.
     *
     * GET /api/auth/me
     */
    @GetMapping("/me")
    @Operation(summary = "Get Current User", description = "Get information about the currently authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current user information"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<LoginResponse> getCurrentUser() {
        LoginResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(response);
    }

    /**
     * Validate if a token is still valid (for client-side checks).
     *
     * GET /api/auth/validate
     */
    @GetMapping("/validate")
    @Operation(summary = "Validate Token", description = "Check if the current token is still valid")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    })
    public ResponseEntity<Map<String, Object>> validateToken(HttpServletRequest request) {
        String jwt = getJwtFromRequest(request);

        Map<String, Object> response = new HashMap<>();

        if (!StringUtils.hasText(jwt)) {
            response.put("valid", false);
            response.put("message", "No token provided");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        if (!tokenProvider.validateToken(jwt)) {
            response.put("valid", false);
            response.put("message", "Token is invalid or expired");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String tokenJti = tokenProvider.getJtiFromToken(jwt);
        boolean sessionValid = authService.isSessionValid(tokenJti);

        if (!sessionValid) {
            response.put("valid", false);
            response.put("message", "Session has been revoked");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        response.put("valid", true);
        response.put("expiresAt", tokenProvider.getExpirationDateFromToken(jwt).toInstant().toString());
        return ResponseEntity.ok(response);
    }

    // ============ Private Helper Methods ============

    /**
     * Extract JWT token from Authorization header
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    /**
     * Get client IP address, considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            // X-Forwarded-For may contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
