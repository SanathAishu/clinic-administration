package com.clinic.mgmt.visit.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.clinic.mgmt.visit.dto.HouseVisitRequest;
import com.clinic.mgmt.visit.dto.HouseVisitResponse;
import com.clinic.mgmt.visit.mapper.HouseVisitMapper;
import com.clinic.mgmt.visit.service.HouseVisitService;
import jakarta.validation.Valid;
import java.time.Instant;
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
@RequestMapping("/api/v1/house-visits")
public class HouseVisitController {

	private final HouseVisitService service;
	private final HouseVisitMapper mapper;

	public HouseVisitController(HouseVisitService service, HouseVisitMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('house_visits.read')")
	public List<HouseVisitResponse> list(
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "provider_id", required = false) String providerId,
			@RequestParam(value = "scheduled_from", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant scheduledFrom,
			@RequestParam(value = "scheduled_to", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant scheduledTo
	) {
		String organizationId = SecurityUtils.currentOrganizationId();
		return service.list(organizationId, status, providerId, scheduledFrom, scheduledTo).stream()
				.map(mapper::toResponse)
				.toList();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('house_visits.read')")
	public HouseVisitResponse get(@PathVariable String id) {
		return mapper.toResponse(service.get(id, SecurityUtils.currentOrganizationId()));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('house_visits.create')")
	public HouseVisitResponse create(
			@Valid @RequestBody HouseVisitRequest request,
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
	@PreAuthorize("hasAuthority('house_visits.update')")
	public HouseVisitResponse update(
			@PathVariable String id,
			@Valid @RequestBody HouseVisitRequest request,
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
	@PreAuthorize("hasAuthority('house_visits.delete')")
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
