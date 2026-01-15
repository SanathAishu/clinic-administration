package com.clinic.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String ISSUER = "clinic-administration";
    private static final long ACCESS_TOKEN_EXPIRY_MS = 15 * 60 * 1000; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000L; // 7 days

    private final SecretKey secretKey;

    @Getter
    private final long accessTokenExpirationMs;

    @Getter
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${clinic.jwt.secret}") String secret,
            @Value("${clinic.jwt.expiration:#{900000}}") long jwtExpirationMs,
            @Value("${clinic.jwt.refresh-expiration:#{604800000}}") long refreshExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        // Use 15 min for access token as per requirements (override config if needed)
        this.accessTokenExpirationMs = ACCESS_TOKEN_EXPIRY_MS;
        // Use 7 days for refresh token as per requirements
        this.refreshTokenExpirationMs = REFRESH_TOKEN_EXPIRY_MS;
    }

    /**
     * Token generation result containing both the token and its JTI
     */
    public record TokenResult(String token, String jti, Instant expiresAt) {}

    /**
     * Generate access token with JTI for session tracking
     */
    public TokenResult generateAccessToken(UserPrincipal userPrincipal, UUID tenantId, List<String> roles) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(accessTokenExpirationMs, ChronoUnit.MILLIS);
        String jti = UUID.randomUUID().toString();

        String rolesString = roles != null ? String.join(",", roles) : "";

        String token = Jwts.builder()
                .subject(userPrincipal.getId().toString())
                .claim("email", userPrincipal.getEmail())
                .claim("tenantId", tenantId.toString())
                .claim("roles", rolesString)
                .claim("jti", jti)
                .issuer(ISSUER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();

        return new TokenResult(token, jti, expiryDate);
    }

    /**
     * Generate access token from Authentication object
     */
    public TokenResult generateAccessToken(Authentication authentication, UUID tenantId) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5)) // Remove ROLE_ prefix
                .collect(Collectors.toList());

        return generateAccessToken(userPrincipal, tenantId, roles);
    }

    /**
     * Generate refresh token with JTI for session tracking
     */
    public TokenResult generateRefreshToken(UUID userId, UUID tenantId) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(refreshTokenExpirationMs, ChronoUnit.MILLIS);
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("jti", jti)
                .claim("type", "refresh")
                .issuer(ISSUER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();

        return new TokenResult(token, jti, expiryDate);
    }

    /**
     * Get user ID from JWT token
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.getSubject());
    }

    /**
     * Get tenant ID from JWT token
     */
    public UUID getTenantIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.get("tenantId", String.class));
    }

    /**
     * Get JWT ID (jti) from token
     */
    public String getJtiFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("jti", String.class);
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }

    /**
     * Get expiration date from token
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getExpiration();
    }

    /**
     * Get expiration instant from token
     */
    public Instant getExpirationInstantFromToken(String token) {
        return getExpirationDateFromToken(token).toInstant();
    }

    /**
     * Get all claims from token
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get roles from token
     */
    public List<String> getRolesFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        String rolesString = claims.get("roles", String.class);
        if (rolesString == null || rolesString.isEmpty()) {
            return List.of();
        }
        return List.of(rolesString.split(","));
    }

    /**
     * Check if token is a refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String type = claims.get("type", String.class);
            return "refresh".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if token is expired (without throwing exception)
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }
}
