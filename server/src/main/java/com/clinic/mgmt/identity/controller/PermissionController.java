package com.clinic.mgmt.identity.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import com.clinic.mgmt.identity.dto.PermissionRequest;
import com.clinic.mgmt.identity.dto.PermissionResponse;
import com.clinic.mgmt.identity.mapper.PermissionMapper;
import com.clinic.mgmt.identity.service.PermissionService;
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
@RequestMapping("/api/v1/permissions")
public class PermissionController {

	private final PermissionService permissionService;
	private final PermissionMapper permissionMapper;

	public PermissionController(PermissionService permissionService, PermissionMapper permissionMapper) {
		this.permissionService = permissionService;
		this.permissionMapper = permissionMapper;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('permissions.read')")
	public List<PermissionResponse> list(
			@RequestParam(value = "organization_id", required = false) String organizationId,
			@RequestParam(value = "active", required = false) Boolean active,
			@RequestParam(value = "resource", required = false) String resource,
			@RequestParam(value = "action", required = false) String action,
			@RequestParam(value = "include_system", required = false) Boolean includeSystem
	) {
		return permissionService.list(
						organizationId,
						SecurityUtils.currentOrganizationId(),
						active,
						resource,
						action,
						includeSystem
				).stream()
				.map(permissionMapper::toResponse)
				.toList();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('permissions.read')")
	public PermissionResponse get(@PathVariable String id) {
		return permissionMapper.toResponse(permissionService.get(id, SecurityUtils.currentOrganizationId()));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('permissions.create')")
	public PermissionResponse create(@Valid @RequestBody PermissionRequest request, HttpServletRequest httpRequest) {
		String actorUserId = SecurityUtils.currentUserId();
		return permissionMapper.toResponse(permissionService.create(
				request,
				actorUserId,
				SecurityUtils.currentOrganizationId(),
				clientIp(httpRequest),
				userAgent(httpRequest)
		));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('permissions.update')")
	public PermissionResponse update(
			@PathVariable String id,
			@Valid @RequestBody PermissionRequest request,
			HttpServletRequest httpRequest
	) {
		String actorUserId = SecurityUtils.currentUserId();
		return permissionMapper.toResponse(permissionService.update(
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
	@PreAuthorize("hasAuthority('permissions.delete')")
	public void delete(@PathVariable String id, HttpServletRequest httpRequest) {
		String actorUserId = SecurityUtils.currentUserId();
		permissionService.deactivate(
				id,
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
