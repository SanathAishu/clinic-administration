package com.clinic.mgmt.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.auth.domain.RefreshToken;
import com.clinic.mgmt.auth.repository.RefreshTokenRepository;
import com.clinic.mgmt.identity.domain.User;
import com.clinic.mgmt.security.JwtProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RefreshTokenService {

	private final RefreshTokenRepository repository;
	private final JwtProperties jwtProperties;
	private final AuditLogService auditLogService;
	private final SecureRandom secureRandom = new SecureRandom();

	public RefreshTokenService(
			RefreshTokenRepository repository,
			JwtProperties jwtProperties,
			AuditLogService auditLogService
	) {
		this.repository = repository;
		this.jwtProperties = jwtProperties;
		this.auditLogService = auditLogService;
	}

	public IssueResult issue(User user, String ipAddress, String userAgent, String actorUserId) {
		String rawToken = generateToken();
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setId(UUID.randomUUID().toString());
		refreshToken.setUserId(user.getId());
		refreshToken.setTokenHash(hashToken(rawToken));
		refreshToken.setExpiresAt(Instant.now().plus(jwtProperties.getRefreshTokenTtl()));
		refreshToken.setIpAddress(ipAddress);
		refreshToken.setUserAgent(userAgent);
		repository.save(refreshToken);

		auditLogService.record(
				actorUserId,
				"create",
				"refresh_tokens",
				List.of(refreshToken.getId()),
				ipAddress,
				userAgent
		);

		return new IssueResult(rawToken, refreshToken);
	}

	public IssueResult rotate(String rawToken, String ipAddress, String userAgent, String actorUserId) {
		return rotate(findValid(rawToken), ipAddress, userAgent, actorUserId);
	}

	public IssueResult rotate(RefreshToken existing, String ipAddress, String userAgent, String actorUserId) {
		String replacementRaw = generateToken();
		RefreshToken replacement = new RefreshToken();
		replacement.setId(UUID.randomUUID().toString());
		replacement.setUserId(existing.getUserId());
		replacement.setTokenHash(hashToken(replacementRaw));
		replacement.setExpiresAt(Instant.now().plus(jwtProperties.getRefreshTokenTtl()));
		replacement.setIpAddress(ipAddress);
		replacement.setUserAgent(userAgent);
		repository.save(replacement);

		auditLogService.record(
				actorUserId,
				"create",
				"refresh_tokens",
				List.of(replacement.getId()),
				ipAddress,
				userAgent
		);

		existing.setRevokedAt(Instant.now());
		existing.setReplacedBy(replacement.getId());
		repository.save(existing);

		auditLogService.record(
				actorUserId,
				"update",
				"refresh_tokens",
				List.of(existing.getId()),
				ipAddress,
				userAgent
		);

		return new IssueResult(replacementRaw, replacement);
	}

	public RefreshToken requireValid(String rawToken) {
		return findValid(rawToken);
	}

	public void revoke(String rawToken, String ipAddress, String userAgent, String actorUserId) {
		RefreshToken token = findValid(rawToken);
		token.setRevokedAt(Instant.now());
		repository.save(token);
		auditLogService.record(
				actorUserId,
				"update",
				"refresh_tokens",
				List.of(token.getId()),
				ipAddress,
				userAgent
		);
	}

	public String hashToken(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to hash token", ex);
		}
	}

	private String generateToken() {
		byte[] bytes = new byte[jwtProperties.getRefreshTokenBytes()];
		secureRandom.nextBytes(bytes);
		return HexFormat.of().formatHex(bytes);
	}

	private RefreshToken findValid(String rawToken) {
		String hash = hashToken(rawToken);
		Optional<RefreshToken> token = repository.findByTokenHash(hash);
		RefreshToken existing = token.orElseThrow(
				() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token")
		);
		if (existing.getRevokedAt() != null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token revoked");
		}
		if (existing.getExpiresAt() != null && existing.getExpiresAt().isBefore(Instant.now())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
		}
		return existing;
	}

	public static class IssueResult {

		private final String rawToken;
		private final RefreshToken entity;

		public IssueResult(String rawToken, RefreshToken entity) {
			this.rawToken = rawToken;
			this.entity = entity;
		}

		public String getRawToken() {
			return rawToken;
		}

		public RefreshToken getEntity() {
			return entity;
		}
	}
}
