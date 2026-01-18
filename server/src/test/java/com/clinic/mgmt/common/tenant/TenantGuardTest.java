package com.clinic.mgmt.common.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import com.clinic.mgmt.common.exception.InvalidRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TenantGuardTest {

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void resolveOrganizationId_allowsSuperAdminCrossTenant() {
		setPermissions(List.of("system.super_admin"));

		String resolved = TenantGuard.resolveOrganizationId("org-beta", "org-alpha");

		assertEquals("org-beta", resolved);
	}

	@Test
	void resolveOrganizationId_throwsOnOrgMismatch() {
		setPermissions(List.of());

		assertThrows(
				InvalidRequestException.class,
				() -> TenantGuard.resolveOrganizationId("org-beta", "org-alpha")
		);
	}

	private void setPermissions(List<String> permissions) {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.claim("permissions", permissions)
				.claim("org_id", "org-alpha")
				.subject("user-1")
				.build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
	}
}
