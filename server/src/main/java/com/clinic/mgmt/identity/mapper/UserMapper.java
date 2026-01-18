package com.clinic.mgmt.identity.mapper;

import com.clinic.mgmt.identity.domain.User;
import com.clinic.mgmt.identity.dto.UserCreateRequest;
import com.clinic.mgmt.identity.dto.UserResponse;
import com.clinic.mgmt.identity.dto.UserUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

	public User toNewEntity(UserCreateRequest request) {
		User user = new User();
		user.setOrganizationId(request.getOrganizationId());
		user.setFullName(request.getFullName());
		user.setEmail(request.getEmail());
		user.setPhone(request.getPhone());
		if (request.getStatus() != null) {
			user.setStatus(request.getStatus());
		}
		return user;
	}

	public void applyUpdate(User user, UserUpdateRequest request) {
		if (request.getFullName() != null) {
			user.setFullName(request.getFullName());
		}
		if (request.getEmail() != null) {
			user.setEmail(request.getEmail());
		}
		if (request.getPhone() != null) {
			user.setPhone(request.getPhone());
		}
		if (request.getStatus() != null) {
			user.setStatus(request.getStatus());
		}
	}

	public UserResponse toResponse(User user) {
		UserResponse response = new UserResponse();
		response.setId(user.getId());
		response.setOrganizationId(user.getOrganizationId());
		response.setFullName(user.getFullName());
		response.setEmail(user.getEmail());
		response.setPhone(user.getPhone());
		response.setStatus(user.getStatus());
		response.setRoleIds(user.getRoleIds());
		response.setRoleCodes(user.getRoleCodes());
		response.setCreatedAt(user.getCreatedAt());
		response.setUpdatedAt(user.getUpdatedAt());
		return response;
	}
}
