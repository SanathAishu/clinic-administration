package com.clinic.mgmt.security;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityUtils {

	private SecurityUtils() {
	}

	public static String currentUserId() {
		Jwt jwt = currentJwt();
		return jwt == null ? null : jwt.getSubject();
	}

	public static String currentOrganizationId() {
		Jwt jwt = currentJwt();
		return jwt == null ? null : jwt.getClaimAsString("org_id");
	}

	public static List<String> currentPermissions() {
		Jwt jwt = currentJwt();
		return jwt == null ? List.of() : jwt.getClaimAsStringList("permissions");
	}

	private static Jwt currentJwt() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken jwtAuth) {
			return jwtAuth.getToken();
		}
		return null;
	}
}
