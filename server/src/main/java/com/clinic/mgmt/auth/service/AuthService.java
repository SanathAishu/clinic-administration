package com.clinic.mgmt.auth.service;

import java.util.List;
import java.util.Optional;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.auth.domain.RefreshToken;
import com.clinic.mgmt.auth.dto.AuthUserResponse;
import com.clinic.mgmt.auth.dto.ChangePasswordRequest;
import com.clinic.mgmt.auth.dto.LoginRequest;
import com.clinic.mgmt.auth.dto.LogoutRequest;
import com.clinic.mgmt.auth.dto.RefreshTokenRequest;
import com.clinic.mgmt.auth.dto.TokenResponse;
import com.clinic.mgmt.identity.domain.User;
import com.clinic.mgmt.identity.repository.UserRepository;
import com.clinic.mgmt.identity.service.PermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	private final RefreshTokenService refreshTokenService;
	private final PermissionService permissionService;
	private final AuditLogService auditLogService;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			TokenService tokenService,
			RefreshTokenService refreshTokenService,
			PermissionService permissionService,
			AuditLogService auditLogService
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.refreshTokenService = refreshTokenService;
		this.permissionService = permissionService;
		this.auditLogService = auditLogService;
	}

	public TokenResponse login(LoginRequest request, String ipAddress, String userAgent) {
		String identifier = request.getIdentifier().trim();
		Optional<User> byEmail = userRepository.findByEmailIgnoreCase(identifier);
		User user = byEmail.or(() -> userRepository.findByPhone(identifier))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
		ensureActive(user);
		if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}

		List<String> permissions = permissionService.resolvePermissionCodes(user.getId());
		String accessToken = tokenService.createAccessToken(user, permissions);
		RefreshTokenService.IssueResult refreshIssue = refreshTokenService.issue(
				user,
				ipAddress,
				userAgent,
				user.getId()
		);

		TokenResponse response = new TokenResponse();
		response.setAccessToken(accessToken);
		response.setRefreshToken(refreshIssue.getRawToken());
		response.setTokenType("Bearer");
		response.setExpiresIn(tokenService.accessTokenTtlSeconds());
		response.setUser(toAuthUserResponse(user, permissions));
		return response;
	}

	public TokenResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent) {
		RefreshToken token = refreshTokenService.requireValid(request.getRefreshToken());
		User user = userRepository.findById(token.getUserId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		ensureActive(user);

		RefreshTokenService.IssueResult refreshIssue = refreshTokenService.rotate(
				token,
				ipAddress,
				userAgent,
				user.getId()
		);
		List<String> permissions = permissionService.resolvePermissionCodes(user.getId());
		String accessToken = tokenService.createAccessToken(user, permissions);
		TokenResponse response = new TokenResponse();
		response.setAccessToken(accessToken);
		response.setRefreshToken(refreshIssue.getRawToken());
		response.setTokenType("Bearer");
		response.setExpiresIn(tokenService.accessTokenTtlSeconds());
		response.setUser(toAuthUserResponse(user, permissions));
		return response;
	}

	public void logout(LogoutRequest request, String ipAddress, String userAgent) {
		RefreshToken token = refreshTokenService.requireValid(request.getRefreshToken());
		refreshTokenService.revoke(request.getRefreshToken(), ipAddress, userAgent, token.getUserId());
	}

	public AuthUserResponse me(String userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		List<String> permissions = permissionService.resolvePermissionCodes(user.getId());
		return toAuthUserResponse(user, permissions);
	}

	public void changePassword(String userId, ChangePasswordRequest request, String ipAddress, String userAgent) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}
		user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);
		auditLogService.record(
				userId,
				"update",
				"users",
				List.of(user.getId()),
				ipAddress,
				userAgent
		);
	}

	private void ensureActive(User user) {
		if (!"active".equalsIgnoreCase(user.getStatus())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is inactive");
		}
	}

	private AuthUserResponse toAuthUserResponse(User user, List<String> permissions) {
		AuthUserResponse response = new AuthUserResponse();
		response.setId(user.getId());
		response.setOrganizationId(user.getOrganizationId());
		response.setFullName(user.getFullName());
		response.setEmail(user.getEmail());
		response.setPhone(user.getPhone());
		response.setStatus(user.getStatus());
		response.setRoleIds(user.getRoleIds());
		response.setRoleCodes(user.getRoleCodes());
		response.setPermissions(permissions);
		return response;
	}
}
