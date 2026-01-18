package com.clinic.mgmt.reference.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.common.exception.InvalidRequestException;
import com.clinic.mgmt.common.tenant.TenantGuard;
import com.clinic.mgmt.reference.domain.Clinic;
import com.clinic.mgmt.reference.dto.ClinicRequest;
import com.clinic.mgmt.reference.exception.ClinicNotFoundException;
import com.clinic.mgmt.reference.exception.DuplicateClinicException;
import com.clinic.mgmt.reference.mapper.ClinicMapper;
import com.clinic.mgmt.reference.repository.ClinicRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class ClinicService {

	private final ClinicRepository repository;
	private final ClinicMapper mapper;
	private final MongoTemplate mongoTemplate;
	private final AuditLogService auditLogService;

	public ClinicService(
			ClinicRepository repository,
			ClinicMapper mapper,
			MongoTemplate mongoTemplate,
			AuditLogService auditLogService
	) {
		this.repository = repository;
		this.mapper = mapper;
		this.mongoTemplate = mongoTemplate;
		this.auditLogService = auditLogService;
	}

	public List<Clinic> list(String requestOrganizationId, String actorOrganizationId, Boolean active) {
		String scopedOrganizationId = TenantGuard.resolveScope(requestOrganizationId, actorOrganizationId);
		Query query = new Query();
		if (scopedOrganizationId != null) {
			query.addCriteria(Criteria.where("organization_id").is(scopedOrganizationId));
		}
		if (active != null) {
			query.addCriteria(Criteria.where("active").is(active));
		}
		return mongoTemplate.find(query, Clinic.class);
	}

	public Clinic get(String id, String organizationId) {
		Clinic clinic = repository.findById(id)
				.orElseThrow(() -> new ClinicNotFoundException("Clinic not found"));
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		if (scopedOrganizationId != null && !scopedOrganizationId.equals(clinic.getOrganizationId())) {
			throw new ClinicNotFoundException("Clinic not found");
		}
		return clinic;
	}

	public Clinic create(
			ClinicRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		Optional<Clinic> existing = repository.findByOrganizationIdAndCode(
				resolvedOrganizationId,
				request.getCode()
		);
		if (existing.isPresent()) {
			throw new DuplicateClinicException("Clinic code already exists");
		}
		Clinic clinic = mapper.toNewEntity(request);
		clinic.setId(UUID.randomUUID().toString());
		clinic.setOrganizationId(resolvedOrganizationId);
		Clinic saved = repository.save(clinic);
		auditLogService.record(
				actorUserId,
				"create",
				"clinics",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public Clinic update(
			String id,
			ClinicRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		Clinic clinic = get(id, organizationId);
		if (!resolvedOrganizationId.equals(clinic.getOrganizationId())) {
			throw new InvalidRequestException("organization_id cannot be changed for clinics");
		}
		Optional<Clinic> existing = repository.findByOrganizationIdAndCode(
				resolvedOrganizationId,
				request.getCode()
		);
		if (existing.isPresent() && !existing.get().getId().equals(clinic.getId())) {
			throw new DuplicateClinicException("Clinic code already exists");
		}
		mapper.applyRequest(clinic, request, false);
		clinic.setOrganizationId(resolvedOrganizationId);
		Clinic saved = repository.save(clinic);
		auditLogService.record(
				actorUserId,
				"update",
				"clinics",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public void deactivate(
			String id,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		Clinic clinic = get(id, organizationId);
		clinic.setActive(false);
		repository.save(clinic);
		auditLogService.record(
				actorUserId,
				"delete",
				"clinics",
				List.of(clinic.getId()),
				ipAddress,
				userAgent
		);
	}
}
