package com.clinic.mgmt.treatment.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.clinic.mgmt.treatment.dto.TreatmentTypeRequest;
import com.clinic.mgmt.treatment.dto.TreatmentTypeResponse;
import com.clinic.mgmt.treatment.mapper.TreatmentTypeMapper;
import com.clinic.mgmt.treatment.service.TreatmentTypeService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/treatment-types")
public class TreatmentTypeController {

	private final TreatmentTypeService service;
	private final TreatmentTypeMapper mapper;

	public TreatmentTypeController(TreatmentTypeService service, TreatmentTypeMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('treatment_types.read')")
	public List<TreatmentTypeResponse> list(
			@RequestParam(value = "clinic_id", required = false) String clinicId,
			@RequestParam(value = "active", required = false) Boolean active
	) {
		String organizationId = SecurityUtils.currentOrganizationId();
		return service.list(organizationId, clinicId, active).stream()
				.map(mapper::toResponse)
				.toList();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('treatment_types.read')")
	public TreatmentTypeResponse get(@PathVariable String id) {
		return mapper.toResponse(service.get(id, SecurityUtils.currentOrganizationId()));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('treatment_types.create')")
	public TreatmentTypeResponse create(
			@Valid @RequestBody TreatmentTypeRequest request,
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
	@PreAuthorize("hasAuthority('treatment_types.update')")
	public TreatmentTypeResponse update(
			@PathVariable String id,
			@Valid @RequestBody TreatmentTypeRequest request,
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
	@PreAuthorize("hasAuthority('treatment_types.delete')")
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
