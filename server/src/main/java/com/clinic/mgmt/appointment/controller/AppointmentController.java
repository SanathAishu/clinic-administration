package com.clinic.mgmt.appointment.controller;

import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import com.clinic.mgmt.appointment.dto.AppointmentRequest;
import com.clinic.mgmt.appointment.dto.AppointmentResponse;
import com.clinic.mgmt.appointment.mapper.AppointmentMapper;
import com.clinic.mgmt.appointment.service.AppointmentService;
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
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

	private final AppointmentService service;
	private final AppointmentMapper mapper;

	public AppointmentController(AppointmentService service, AppointmentMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('appointments.read')")
	public List<AppointmentResponse> list(
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "scheduled_from", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant scheduledFrom,
			@RequestParam(value = "scheduled_to", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant scheduledTo,
			@RequestParam(value = "clinic_id", required = false) String clinicId,
			@RequestParam(value = "branch_id", required = false) String branchId,
			@RequestParam(value = "patient_id", required = false) String patientId,
			@RequestParam(value = "provider_id", required = false) String providerId
	) {
		String organizationId = SecurityUtils.currentOrganizationId();
		return service.list(
						organizationId,
						status,
						scheduledFrom,
						scheduledTo,
						clinicId,
						branchId,
						patientId,
						providerId
				)
				.stream()
				.map(mapper::toResponse)
				.toList();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('appointments.read')")
	public AppointmentResponse get(@PathVariable String id) {
		return mapper.toResponse(service.get(id, SecurityUtils.currentOrganizationId()));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('appointments.create')")
	public AppointmentResponse create(
			@Valid @RequestBody AppointmentRequest request,
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
	@PreAuthorize("hasAuthority('appointments.update')")
	public AppointmentResponse update(
			@PathVariable String id,
			@Valid @RequestBody AppointmentRequest request,
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
	@PreAuthorize("hasAuthority('appointments.delete')")
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
