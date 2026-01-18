package com.clinic.mgmt.identity.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import com.clinic.mgmt.identity.dto.RolePermissionUpdateRequest;
import com.clinic.mgmt.identity.dto.RoleRequest;
import com.clinic.mgmt.identity.dto.RoleResponse;
import com.clinic.mgmt.identity.dto.PermissionResponse;
import com.clinic.mgmt.identity.mapper.PermissionMapper;
import com.clinic.mgmt.identity.mapper.RoleMapper;
import com.clinic.mgmt.identity.service.RolePermissionService;
import com.clinic.mgmt.identity.service.RoleService;
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
@RequestMapping("/api/v1/roles")
public class RoleController {

	private final RoleService roleService;
	private final RolePermissionService rolePermissionService;
	private final RoleMapper roleMapper;
	private final PermissionMapper permissionMapper;

	public RoleController(
			RoleService roleService,
			RolePermissionService rolePermissionService,
			RoleMapper roleMapper,
			PermissionMapper permissionMapper
	) {
		this.roleService = roleService;
		this.rolePermissionService = rolePermissionService;
		this.roleMapper = roleMapper;
		this.permissionMapper = permissionMapper;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('roles.read')")
	public List<RoleResponse> list(@RequestParam(value = "organization_id", required = false) String organizationId) {
		return roleService.list(organizationId, SecurityUtils.currentOrganizationId()).stream()
				.map(roleMapper::toResponse)
				.toList();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('roles.read')")
	public RoleResponse get(@PathVariable String id) {
		return roleMapper.toResponse(roleService.get(id, SecurityUtils.currentOrganizationId()));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('roles.create')")
	public RoleResponse create(@Valid @RequestBody RoleRequest request, HttpServletRequest httpRequest) {
		String actorUserId = SecurityUtils.currentUserId();
		return roleMapper.toResponse(roleService.create(
				request,
				actorUserId,
				SecurityUtils.currentOrganizationId(),
				clientIp(httpRequest),
				userAgent(httpRequest)
		));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('roles.update')")
	public RoleResponse update(
			@PathVariable String id,
			@Valid @RequestBody RoleRequest request,
			HttpServletRequest httpRequest
	) {
		String actorUserId = SecurityUtils.currentUserId();
		return roleMapper.toResponse(roleService.update(
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
	@PreAuthorize("hasAuthority('roles.delete')")
	public void delete(@PathVariable String id, HttpServletRequest httpRequest) {
		String actorUserId = SecurityUtils.currentUserId();
		roleService.delete(
				id,
				actorUserId,
				SecurityUtils.currentOrganizationId(),
				clientIp(httpRequest),
				userAgent(httpRequest)
		);
	}

	@GetMapping("/{id}/permissions")
	@PreAuthorize("hasAuthority('roles.read')")
	public List<PermissionResponse> listPermissions(@PathVariable String id) {
		return rolePermissionService.listPermissions(id, SecurityUtils.currentOrganizationId()).stream()
				.map(permissionMapper::toResponse)
				.toList();
	}

	@PutMapping("/{id}/permissions")
	@PreAuthorize("hasAuthority('roles.update')")
	public List<PermissionResponse> replacePermissions(
			@PathVariable String id,
			@Valid @RequestBody RolePermissionUpdateRequest request,
			HttpServletRequest httpRequest
	) {
		String actorUserId = SecurityUtils.currentUserId();
		return rolePermissionService.replacePermissions(
				id,
				request.getPermissionIds(),
				actorUserId,
				SecurityUtils.currentOrganizationId(),
				clientIp(httpRequest),
				userAgent(httpRequest)
		).stream().map(permissionMapper::toResponse).toList();
	}

	@PostMapping("/{id}/permissions")
	@PreAuthorize("hasAuthority('roles.update')")
	public List<PermissionResponse> addPermissions(
			@PathVariable String id,
			@Valid @RequestBody RolePermissionUpdateRequest request,
			HttpServletRequest httpRequest
	) {
		String actorUserId = SecurityUtils.currentUserId();
		return rolePermissionService.addPermissions(
				id,
				request.getPermissionIds(),
				actorUserId,
				SecurityUtils.currentOrganizationId(),
				clientIp(httpRequest),
				userAgent(httpRequest)
		).stream().map(permissionMapper::toResponse).toList();
	}

	@DeleteMapping("/{id}/permissions/{permissionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAuthority('roles.update')")
	public void removePermission(
			@PathVariable String id,
			@PathVariable String permissionId,
			HttpServletRequest httpRequest
	) {
		String actorUserId = SecurityUtils.currentUserId();
		rolePermissionService.removePermission(
				id,
				permissionId,
				actorUserId,
				SecurityUtils.currentOrganizationId(),
				clientIp(httpRequest),
				userAgent(httpRequest)
		);
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
