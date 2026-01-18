package com.clinic.mgmt.common.tenant;

import java.util.List;
import com.clinic.mgmt.common.exception.InvalidRequestException;
import com.clinic.mgmt.security.SecurityUtils;

public final class TenantGuard {

	private static final String SUPER_ADMIN_PERMISSION = "system.super_admin";

	private TenantGuard() {
	}

	public static boolean isSuperAdmin() {
		List<String> permissions = SecurityUtils.currentPermissions();
		return permissions != null && permissions.contains(SUPER_ADMIN_PERMISSION);
	}

	public static String resolveScope(String requestOrganizationId, String actorOrganizationId) {
		if (isSuperAdmin()) {
			if (requestOrganizationId != null && !requestOrganizationId.isBlank()) {
				return requestOrganizationId;
			}
			return null;
		}
		String current = requireOrganizationId(actorOrganizationId);
		if (requestOrganizationId != null && !requestOrganizationId.isBlank()
				&& !current.equals(requestOrganizationId)) {
			throw new InvalidRequestException("organization_id does not match current tenant");
		}
		return current;
	}

	public static String resolveOrganizationId(String requestOrganizationId, String actorOrganizationId) {
		if (isSuperAdmin()) {
			if (requestOrganizationId != null && !requestOrganizationId.isBlank()) {
				return requestOrganizationId;
			}
			return requireOrganizationId(actorOrganizationId);
		}
		requireMatch(requestOrganizationId, actorOrganizationId);
		return actorOrganizationId;
	}

	public static void requireMatch(String requestOrganizationId, String actorOrganizationId) {
		if (isSuperAdmin()) {
			return;
		}
		String current = requireOrganizationId(actorOrganizationId);
		if (requestOrganizationId == null || requestOrganizationId.isBlank()) {
			throw new InvalidRequestException("organization_id is required");
		}
		if (!current.equals(requestOrganizationId)) {
			throw new InvalidRequestException("organization_id does not match current tenant");
		}
	}

	private static String requireOrganizationId(String organizationId) {
		if (organizationId == null || organizationId.isBlank()) {
			throw new InvalidRequestException("organization_id is required for tenant scope");
		}
		return organizationId;
	}
}
