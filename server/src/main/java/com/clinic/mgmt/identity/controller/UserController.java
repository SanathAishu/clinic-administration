package com.clinic.mgmt.identity.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import com.clinic.mgmt.identity.dto.PermissionResponse;
import com.clinic.mgmt.identity.dto.UserCreateRequest;
import com.clinic.mgmt.identity.dto.UserResponse;
import com.clinic.mgmt.identity.dto.UserRoleUpdateRequest;
import com.clinic.mgmt.identity.dto.UserStatusRequest;
import com.clinic.mgmt.identity.dto.UserUpdateRequest;
import com.clinic.mgmt.identity.mapper.PermissionMapper;
import com.clinic.mgmt.identity.mapper.UserMapper;
import com.clinic.mgmt.identity.service.PermissionService;
import com.clinic.mgmt.identity.service.UserService;
import com.clinic.mgmt.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;
	private final PermissionService permissionService;
	private final UserMapper userMapper;
	private final PermissionMapper permissionMapper;

	public UserController(
			UserService userService,
			PermissionService permissionService,
			UserMapper userMapper,
			PermissionMapper permissionMapper
	) {
		this.userService = userService;
		this.permissionService = permissionService;
		this.userMapper = userMapper;
		this.permissionMapper = permissionMapper;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('users.read')")
	public List<UserResponse> list(
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "role_code", required = false) String roleCode,
			@RequestParam(value = "clinic_id", required = false) String clinicId,
			@RequestParam(value = "branch_id", required = false) String branchId
	) {
		String organizationId = SecurityUtils.currentOrganizationId();
		return userService.list(organizationId, status, roleCode, clinicId, branchId).stream()
				.map(userMapper::toResponse)
				.toList();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('users.read')")
	public UserResponse get(@PathVariable String id) {
		return userMapper.toResponse(userService.get(id, SecurityUtils.currentOrganizationId()));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('users.create')")
	public UserResponse create(@Valid @RequestBody UserCreateRequest request, HttpServletRequest httpRequest) {
		String actorUserId = SecurityUtils.currentUserId();
		return userMapper.toResponse(userService.create(
				request,
				actorUserId,
				SecurityUtils.currentOrganizationId(),
				clientIp(httpRequest),
				userAgent(httpRequest)
		));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('users.update')")
	public UserResponse update(
			@PathVariable String id,
			@Valid @RequestBody UserUpdateRequest request,
			HttpServletRequest httpRequest
	) {
		String actorUserId = SecurityUtils.currentUserId();
		return userMapper.toResponse(userService.update(
				id,
				request,
				actorUserId,
				SecurityUtils.currentOrganizationId(),
				clientIp(httpRequest),
				userAgent(httpRequest)
		));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAuthority('users.delete')")
	public void delete(@PathVariable String id, HttpServletRequest httpRequest) {
		String actorUserId = SecurityUtils.currentUserId();
		userService.deactivate(
				id,
				actorUserId,
				SecurityUtils.currentOrganizationId(),
				clientIp(httpRequest),
				userAgent(httpRequest)
		);
	}

	@PutMapping("/{id}/roles")
	@PreAuthorize("hasAuthority('users.assign_roles')")
	public UserResponse updateRoles(
			@PathVariable String id,
			@Valid @RequestBody UserRoleUpdateRequest request,
			HttpServletRequest httpRequest
	) {
		String actorUserId = SecurityUtils.currentUserId();
		return userMapper.toResponse(userService.updateRoles(
				id,
				request,
				actorUserId,
				SecurityUtils.currentOrganizationId(),
				clientIp(httpRequest),
				userAgent(httpRequest)
		));
	}

	@PostMapping("/{id}/status")
	@PreAuthorize("hasAuthority('users.status')")
	public UserResponse updateStatus(
			@PathVariable String id,
			@Valid @RequestBody UserStatusRequest request,
			HttpServletRequest httpRequest
	) {
		String actorUserId = SecurityUtils.currentUserId();
		return userMapper.toResponse(userService.updateStatus(
				id,
				request.getStatus(),
				actorUserId,
				SecurityUtils.currentOrganizationId(),
				clientIp(httpRequest),
				userAgent(httpRequest)
		));
	}

	@GetMapping("/{id}/permissions")
	@PreAuthorize("hasAuthority('users.read')")
	public List<PermissionResponse> userPermissions(@PathVariable String id) {
		userService.get(id, SecurityUtils.currentOrganizationId());
		return permissionService.resolvePermissions(id).stream()
				.map(permissionMapper::toResponse)
				.toList();
	}

	private String clientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private String userAgent(HttpServletRequest request) {
		return request.getHeader("User-Agent");
	}
}
