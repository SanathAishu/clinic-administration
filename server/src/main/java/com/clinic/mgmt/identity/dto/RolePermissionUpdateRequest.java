package com.clinic.mgmt.identity.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class RolePermissionUpdateRequest {

	@NotEmpty
	private List<String> permissionIds;

	public List<String> getPermissionIds() {
		return permissionIds;
	}

	public void setPermissionIds(List<String> permissionIds) {
		this.permissionIds = permissionIds;
	}
}
