package com.clinic.mgmt.common.organization.service;

import java.util.List;
import java.util.Optional;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.common.organization.domain.Organization;
import com.clinic.mgmt.common.organization.dto.OrganizationRequest;
import com.clinic.mgmt.common.organization.exception.DuplicateOrganizationException;
import com.clinic.mgmt.common.organization.exception.OrganizationNotFoundException;
import com.clinic.mgmt.common.organization.mapper.OrganizationMapper;
import com.clinic.mgmt.common.organization.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

@Service
public class OrganizationService {

	private final OrganizationRepository repository;
	private final OrganizationMapper mapper;
	private final AuditLogService auditLogService;

	public OrganizationService(
			OrganizationRepository repository,
			OrganizationMapper mapper,
			AuditLogService auditLogService
	) {
		this.repository = repository;
		this.mapper = mapper;
		this.auditLogService = auditLogService;
	}

	public List<Organization> list(Boolean active) {
		if (active != null) {
			return repository.findByActive(active);
		}
		return repository.findAll();
	}

	public Organization get(String id) {
		return repository.findById(id)
				.orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));
	}

	public Organization create(
			OrganizationRequest request,
			String actorUserId,
			String ipAddress,
			String userAgent
	) {
		Optional<Organization> existing = repository.findByCode(request.getCode());
		if (existing.isPresent()) {
			throw new DuplicateOrganizationException("Organization code already exists");
		}
		Organization organization = mapper.toNewEntity(request);
		Organization saved = repository.save(organization);
		auditLogService.record(
				actorUserId,
				"create",
				"organizations",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public Organization update(
			String id,
			OrganizationRequest request,
			String actorUserId,
			String ipAddress,
			String userAgent
	) {
		Organization organization = get(id);
		Optional<Organization> existing = repository.findByCode(request.getCode());
		if (existing.isPresent() && !existing.get().getId().equals(organization.getId())) {
			throw new DuplicateOrganizationException("Organization code already exists");
		}
		mapper.applyRequest(organization, request, false);
		Organization saved = repository.save(organization);
		auditLogService.record(
				actorUserId,
				"update",
				"organizations",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public void deactivate(String id, String actorUserId, String ipAddress, String userAgent) {
		Organization organization = get(id);
		organization.setActive(false);
		repository.save(organization);
		auditLogService.record(
				actorUserId,
				"delete",
				"organizations",
				List.of(organization.getId()),
				ipAddress,
				userAgent
		);
	}
}
