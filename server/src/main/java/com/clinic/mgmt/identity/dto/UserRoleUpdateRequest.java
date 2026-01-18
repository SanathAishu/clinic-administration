package com.clinic.mgmt.identity.dto;

import java.util.List;

public class UserRoleUpdateRequest {

	private List<String> roleIds;

	private List<String> roleCodes;

	public List<String> getRoleIds() {
		return roleIds;
	}

	public void setRoleIds(List<String> roleIds) {
		this.roleIds = roleIds;
	}

	public List<String> getRoleCodes() {
		return roleCodes;
	}

	public void setRoleCodes(List<String> roleCodes) {
		this.roleCodes = roleCodes;
	}
}
