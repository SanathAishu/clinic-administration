package com.clinic.mgmt.common.organization.domain;

import com.clinic.mgmt.common.domain.BaseEntity;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("organizations")
public class Organization extends BaseEntity {

	private String name;

	@Indexed(unique = true, name = "uk_organizations_code")
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
