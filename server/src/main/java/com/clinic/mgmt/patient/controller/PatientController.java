package com.clinic.mgmt.patient.controller;

import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import com.clinic.mgmt.patient.dto.PatientRequest;
import com.clinic.mgmt.patient.dto.PatientResponse;
import com.clinic.mgmt.patient.mapper.PatientMapper;
import com.clinic.mgmt.patient.service.PatientService;
import jakarta.validation.Valid;
import java.util.List;
import com.clinic.mgmt.security.SecurityUtils;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/patients")
public class PatientController {

	private final PatientService service;
	private final PatientMapper mapper;

	public PatientController(PatientService service, PatientMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('patients.read')")
	public List<PatientResponse> list(
			@RequestParam(value = "phone", required = false) String phone,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "clinic_id", required = false) String clinicId,
			@RequestParam(value = "branch_id", required = false) String branchId,
			@RequestParam(value = "date_from", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
			@RequestParam(value = "date_to", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo
	) {
		String organizationId = SecurityUtils.currentOrganizationId();
		return service.list(organizationId, phone, name, status, clinicId, branchId, dateFrom, dateTo).stream()
				.map(mapper::toResponse)
				.toList();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('patients.read')")
	public PatientResponse get(@PathVariable String id) {
		return mapper.toResponse(service.get(id, SecurityUtils.currentOrganizationId()));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('patients.create')")
	public PatientResponse create(
			@Valid @RequestBody PatientRequest request,
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
	@PreAuthorize("hasAuthority('patients.update')")
	public PatientResponse update(
			@PathVariable String id,
			@Valid @RequestBody PatientRequest request,
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
	@PreAuthorize("hasAuthority('patients.delete')")
	public void delete(
			@PathVariable String id,
			HttpServletRequest httpRequest
	) {
		service.archive(
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
