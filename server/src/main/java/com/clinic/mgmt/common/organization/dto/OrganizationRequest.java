package com.clinic.mgmt.common.organization.dto;

import jakarta.validation.constraints.NotBlank;

public class OrganizationRequest {

	@NotBlank
	private String name;

	@NotBlank
	private String code;

	private Boolean active;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}
}
