package com.clinic.mgmt.identity.domain;

import com.clinic.mgmt.common.domain.BaseEntity;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("role_permissions")
@CompoundIndex(
		name = "uk_role_permissions_role_permission",
		def = "{'role_id': 1, 'permission_id': 1}",
		unique = true
)
@CompoundIndex(
		name = "idx_role_permissions_permission",
		def = "{'permission_id': 1}"
)
public class RolePermission extends BaseEntity {

	@Field("organization_id")
	private String organizationId;

	@Field("role_id")
	private String roleId;

	@Field("permission_id")
	private String permissionId;

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public String getPermissionId() {
		return permissionId;
	}

	public void setPermissionId(String permissionId) {
		this.permissionId = permissionId;
	}

}
