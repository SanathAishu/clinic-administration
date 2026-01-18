package com.clinic.mgmt.identity.mapper;

import com.clinic.mgmt.identity.domain.Permission;
import com.clinic.mgmt.identity.dto.PermissionRequest;
import com.clinic.mgmt.identity.dto.PermissionResponse;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {

	public Permission toNewEntity(PermissionRequest request) {
		Permission permission = new Permission();
		applyRequest(permission, request);
		return permission;
	}

	public void applyRequest(Permission permission, PermissionRequest request) {
		permission.setOrganizationId(request.getOrganizationId());
		permission.setName(request.getName());
		permission.setResource(request.getResource());
		permission.setAction(request.getAction());
		if (request.getScope() != null) {
			permission.setScope(request.getScope());
		}
		permission.setDescription(request.getDescription());
		if (request.getActive() != null) {
			permission.setActive(request.getActive());
		}
	}

	public PermissionResponse toResponse(Permission permission) {
		PermissionResponse response = new PermissionResponse();
		response.setId(permission.getId());
		response.setOrganizationId(permission.getOrganizationId());
		response.setName(permission.getName());
		response.setPermissionCode(permission.getPermissionCode());
		response.setScope(permission.getScope());
		response.setResource(permission.getResource());
		response.setAction(permission.getAction());
		response.setDescription(permission.getDescription());
		response.setActive(permission.getActive());
		response.setCreatedAt(permission.getCreatedAt());
		response.setUpdatedAt(permission.getUpdatedAt());
		return response;
	}
}
