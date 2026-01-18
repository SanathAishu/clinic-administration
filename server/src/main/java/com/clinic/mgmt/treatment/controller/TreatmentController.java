package com.clinic.mgmt.treatment.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.clinic.mgmt.treatment.dto.TreatmentRequest;
import com.clinic.mgmt.treatment.dto.TreatmentResponse;
import com.clinic.mgmt.treatment.mapper.TreatmentMapper;
import com.clinic.mgmt.treatment.service.TreatmentService;
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
@RequestMapping("/api/v1/treatments")
public class TreatmentController {

	private final TreatmentService service;
	private final TreatmentMapper mapper;

	public TreatmentController(TreatmentService service, TreatmentMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('treatments.read')")
	public List<TreatmentResponse> list(
			@RequestParam(value = "patient_id", required = false) String patientId,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "clinic_id", required = false) String clinicId,
			@RequestParam(value = "provider_id", required = false) String providerId
	) {
		String organizationId = SecurityUtils.currentOrganizationId();
		return service.list(organizationId, patientId, status, clinicId, providerId).stream()
				.map(mapper::toResponse)
				.toList();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('treatments.read')")
	public TreatmentResponse get(@PathVariable String id) {
		return mapper.toResponse(service.get(id, SecurityUtils.currentOrganizationId()));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('treatments.create')")
	public TreatmentResponse create(
			@Valid @RequestBody TreatmentRequest request,
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
	@PreAuthorize("hasAuthority('treatments.update')")
	public TreatmentResponse update(
			@PathVariable String id,
			@Valid @RequestBody TreatmentRequest request,
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
	@PreAuthorize("hasAuthority('treatments.delete')")
	public void delete(
			@PathVariable String id,
			HttpServletRequest httpRequest
	) {
		service.cancel(
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
