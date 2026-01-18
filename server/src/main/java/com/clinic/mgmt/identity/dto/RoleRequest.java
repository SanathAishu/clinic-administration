package com.clinic.mgmt.identity.dto;

import jakarta.validation.constraints.NotBlank;

public class RoleRequest {

	@NotBlank
	private String organizationId;

	@NotBlank
	private String name;

	@NotBlank
	private String roleCode;

	private String description;

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRoleCode() {
		return roleCode;
	}

	public void setRoleCode(String roleCode) {
		this.roleCode = roleCode;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
