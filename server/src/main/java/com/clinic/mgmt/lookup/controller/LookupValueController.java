package com.clinic.mgmt.lookup.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import com.clinic.mgmt.lookup.dto.LookupValueRequest;
import com.clinic.mgmt.lookup.dto.LookupValueResponse;
import com.clinic.mgmt.lookup.mapper.LookupValueMapper;
import com.clinic.mgmt.lookup.service.LookupValueService;
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
@RequestMapping("/api/v1/lookup-values")
public class LookupValueController {

	private final LookupValueService service;
	private final LookupValueMapper mapper;

	public LookupValueController(LookupValueService service, LookupValueMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('lookup_values.read')")
	public List<LookupValueResponse> list(
			@RequestParam(value = "lookup_type", required = false) String lookupType,
			@RequestParam(value = "active", required = false) Boolean active
	) {
		return service.list(lookupType, active).stream()
				.map(mapper::toResponse)
				.toList();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('lookup_values.read')")
	public LookupValueResponse get(@PathVariable String id) {
		return mapper.toResponse(service.get(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('lookup_values.create')")
	public LookupValueResponse create(
			@Valid @RequestBody LookupValueRequest request,
			HttpServletRequest httpRequest
	) {
		return mapper.toResponse(service.create(
				request,
				SecurityUtils.currentUserId(),
				clientIp(httpRequest),
				userAgent(httpRequest)
		));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('lookup_values.update')")
	public LookupValueResponse update(
			@PathVariable String id,
			@Valid @RequestBody LookupValueRequest request,
			HttpServletRequest httpRequest
	) {
		return mapper.toResponse(service.update(
				id,
				request,
				SecurityUtils.currentUserId(),
				clientIp(httpRequest),
				userAgent(httpRequest)
		));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAuthority('lookup_values.delete')")
	public void delete(@PathVariable String id, HttpServletRequest httpRequest) {
		service.deactivate(
				id,
				SecurityUtils.currentUserId(),
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
