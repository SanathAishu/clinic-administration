package com.clinic.mgmt.identity.mapper;

import com.clinic.mgmt.identity.domain.Role;
import com.clinic.mgmt.identity.dto.RoleRequest;
import com.clinic.mgmt.identity.dto.RoleResponse;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

	public Role toNewEntity(RoleRequest request) {
		Role role = new Role();
		applyRequest(role, request);
		return role;
	}

	public void applyRequest(Role role, RoleRequest request) {
		role.setOrganizationId(request.getOrganizationId());
		role.setName(request.getName());
		role.setRoleCode(request.getRoleCode());
		role.setDescription(request.getDescription());
	}

	public RoleResponse toResponse(Role role) {
		RoleResponse response = new RoleResponse();
		response.setId(role.getId());
		response.setOrganizationId(role.getOrganizationId());
		response.setName(role.getName());
		response.setRoleCode(role.getRoleCode());
		response.setDescription(role.getDescription());
		response.setCreatedAt(role.getCreatedAt());
		response.setUpdatedAt(role.getUpdatedAt());
		return response;
	}
}
