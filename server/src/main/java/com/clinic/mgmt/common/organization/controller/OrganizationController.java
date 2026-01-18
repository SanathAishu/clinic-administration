package com.clinic.mgmt.common.organization.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import com.clinic.mgmt.common.organization.dto.OrganizationRequest;
import com.clinic.mgmt.common.organization.dto.OrganizationResponse;
import com.clinic.mgmt.common.organization.mapper.OrganizationMapper;
import com.clinic.mgmt.common.organization.service.OrganizationService;
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
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

	private final OrganizationService service;
	private final OrganizationMapper mapper;

	public OrganizationController(OrganizationService service, OrganizationMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('organizations.read')")
	public List<OrganizationResponse> list(
			@RequestParam(value = "active", required = false) Boolean active
	) {
		return service.list(active, SecurityUtils.currentOrganizationId()).stream()
				.map(mapper::toResponse)
				.toList();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('organizations.read')")
	public OrganizationResponse get(@PathVariable String id) {
		return mapper.toResponse(service.get(id, SecurityUtils.currentOrganizationId()));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('organizations.create') and hasAuthority('system.super_admin')")
	public OrganizationResponse create(
			@Valid @RequestBody OrganizationRequest request,
			HttpServletRequest httpRequest
	) {
		return mapper.toResponse(service.create(
				request,
				SecurityUtils.currentUserId(),
				SecurityUtils.currentOrganizationId(),
				clientIp(httpRequest),
				userAgent(httpRequest)
		));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('organizations.update')")
	public OrganizationResponse update(
			@PathVariable String id,
			@Valid @RequestBody OrganizationRequest request,
			HttpServletRequest httpRequest
	) {
		return mapper.toResponse(service.update(
				id,
				request,
				SecurityUtils.currentUserId(),
				SecurityUtils.currentOrganizationId(),
				clientIp(httpRequest),
				userAgent(httpRequest)
		));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAuthority('organizations.delete') and hasAuthority('system.super_admin')")
	public void delete(@PathVariable String id, HttpServletRequest httpRequest) {
		service.deactivate(
				id,
				SecurityUtils.currentUserId(),
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
