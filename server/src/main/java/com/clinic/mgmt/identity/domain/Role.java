package com.clinic.mgmt.identity.domain;

import com.clinic.mgmt.common.domain.BaseEntity;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("roles")
@CompoundIndex(
		name = "uk_roles_org_name",
		def = "{'organization_id': 1, 'name': 1}",
		unique = true
)
@CompoundIndex(
		name = "uk_roles_org_code",
		def = "{'organization_id': 1, 'role_code': 1}",
		unique = true
)
public class Role extends BaseEntity {

	@Field("organization_id")
	private String organizationId;

	private String name;

	@Field("role_code")
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
