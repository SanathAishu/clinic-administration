package com.clinic.mgmt.identity.domain;

import com.clinic.mgmt.common.domain.BaseEntity;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("permissions")
@CompoundIndex(
		name = "uk_permissions_org_code",
		def = "{'organization_id': 1, 'permission_code': 1}",
		unique = true
)
@CompoundIndex(
		name = "idx_permissions_resource_action",
		def = "{'organization_id': 1, 'resource': 1, 'action': 1}"
)
public class Permission extends BaseEntity {

	@Field("organization_id")
	private String organizationId;

	private String name;

	@Field("permission_code")
	private String permissionCode;

	@Field("scope")
	private String scope;

	private String resource;

	private String action;

	private String description;

	private Boolean active;

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

	public String getPermissionCode() {
		return permissionCode;
	}

	public void setPermissionCode(String permissionCode) {
		this.permissionCode = permissionCode;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

}
