package com.clinic.mgmt.reference.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import com.clinic.mgmt.reference.dto.DepartmentRequest;
import com.clinic.mgmt.reference.dto.DepartmentResponse;
import com.clinic.mgmt.reference.mapper.DepartmentMapper;
import com.clinic.mgmt.reference.service.DepartmentService;
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
@RequestMapping("/api/v1/departments")
public class DepartmentController {

	private final DepartmentService service;
	private final DepartmentMapper mapper;

	public DepartmentController(DepartmentService service, DepartmentMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('departments.read')")
	public List<DepartmentResponse> list(
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
	@PreAuthorize("hasAuthority('departments.read')")
	public DepartmentResponse get(@PathVariable String id) {
		return mapper.toResponse(service.get(id, SecurityUtils.currentOrganizationId()));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('departments.create')")
	public DepartmentResponse create(
			@Valid @RequestBody DepartmentRequest request,
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
	@PreAuthorize("hasAuthority('departments.update')")
	public DepartmentResponse update(
			@PathVariable String id,
			@Valid @RequestBody DepartmentRequest request,
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
	@PreAuthorize("hasAuthority('departments.delete')")
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
