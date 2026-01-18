package com.clinic.mgmt.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.clinic.mgmt.auth.dto.AuthUserResponse;
import com.clinic.mgmt.auth.dto.ChangePasswordRequest;
import com.clinic.mgmt.auth.dto.LoginRequest;
import com.clinic.mgmt.auth.dto.LogoutRequest;
import com.clinic.mgmt.auth.dto.RefreshTokenRequest;
import com.clinic.mgmt.auth.dto.TokenResponse;
import com.clinic.mgmt.auth.service.AuthService;
import com.clinic.mgmt.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		return authService.login(request, clientIp(httpRequest), userAgent(httpRequest));
	}

	@PostMapping("/refresh-token")
	public TokenResponse refresh(
			@Valid @RequestBody RefreshTokenRequest request,
			HttpServletRequest httpRequest
	) {
		return authService.refresh(request, clientIp(httpRequest), userAgent(httpRequest));
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@Valid @RequestBody LogoutRequest request, HttpServletRequest httpRequest) {
		authService.logout(request, clientIp(httpRequest), userAgent(httpRequest));
	}

	@GetMapping("/me")
	public AuthUserResponse me() {
		String userId = SecurityUtils.currentUserId();
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
		}
		return authService.me(userId);
	}

	@PostMapping("/change-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
		String userId = SecurityUtils.currentUserId();
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
		}
		authService.changePassword(userId, request, clientIp(httpRequest), userAgent(httpRequest));
	}

	private String clientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private String userAgent(HttpServletRequest request) {
		return request.getHeader("User-Agent");
	}
}
