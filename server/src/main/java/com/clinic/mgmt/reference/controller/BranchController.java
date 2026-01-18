package com.clinic.mgmt.reference.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import com.clinic.mgmt.reference.dto.BranchRequest;
import com.clinic.mgmt.reference.dto.BranchResponse;
import com.clinic.mgmt.reference.mapper.BranchMapper;
import com.clinic.mgmt.reference.service.BranchService;
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
@RequestMapping("/api/v1/branches")
public class BranchController {

	private final BranchService service;
	private final BranchMapper mapper;

	public BranchController(BranchService service, BranchMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('branches.read')")
	public List<BranchResponse> list(
			@RequestParam(value = "organization_id", required = false) String organizationId,
			@RequestParam(value = "clinic_id", required = false) String clinicId,
			@RequestParam(value = "active", required = false) Boolean active
	) {
		String actorOrganizationId = SecurityUtils.currentOrganizationId();
		return service.list(organizationId, actorOrganizationId, clinicId, active).stream()
				.map(mapper::toResponse)
				.toList();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('branches.read')")
	public BranchResponse get(@PathVariable String id) {
		return mapper.toResponse(service.get(id, SecurityUtils.currentOrganizationId()));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('branches.create')")
	public BranchResponse create(
			@Valid @RequestBody BranchRequest request,
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
	@PreAuthorize("hasAuthority('branches.update')")
	public BranchResponse update(
			@PathVariable String id,
			@Valid @RequestBody BranchRequest request,
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
	@PreAuthorize("hasAuthority('branches.delete')")
	public void delete(
			@PathVariable String id,
			HttpServletRequest httpRequest
	) {
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
