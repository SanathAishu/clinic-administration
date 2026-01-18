package com.clinic.mgmt.reference.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import com.clinic.mgmt.reference.dto.ClinicRequest;
import com.clinic.mgmt.reference.dto.ClinicResponse;
import com.clinic.mgmt.reference.mapper.ClinicMapper;
import com.clinic.mgmt.reference.service.ClinicService;
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
@RequestMapping("/api/v1/clinics")
public class ClinicController {

	private final ClinicService service;
	private final ClinicMapper mapper;

	public ClinicController(ClinicService service, ClinicMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('clinics.read')")
	public List<ClinicResponse> list(
			@RequestParam(value = "organization_id", required = false) String organizationId,
			@RequestParam(value = "active", required = false) Boolean active
	) {
		String actorOrganizationId = SecurityUtils.currentOrganizationId();
		return service.list(organizationId, actorOrganizationId, active).stream()
				.map(mapper::toResponse)
				.toList();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('clinics.read')")
	public ClinicResponse get(@PathVariable String id) {
		return mapper.toResponse(service.get(id, SecurityUtils.currentOrganizationId()));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('clinics.create')")
	public ClinicResponse create(
			@Valid @RequestBody ClinicRequest request,
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
	@PreAuthorize("hasAuthority('clinics.update')")
	public ClinicResponse update(
			@PathVariable String id,
			@Valid @RequestBody ClinicRequest request,
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
	@PreAuthorize("hasAuthority('clinics.delete')")
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
