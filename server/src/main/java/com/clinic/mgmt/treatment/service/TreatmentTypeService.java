package com.clinic.mgmt.treatment.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.common.tenant.TenantGuard;
import com.clinic.mgmt.treatment.domain.TreatmentType;
import com.clinic.mgmt.treatment.dto.TreatmentTypeRequest;
import com.clinic.mgmt.treatment.exception.DuplicateTreatmentTypeException;
import com.clinic.mgmt.treatment.exception.TreatmentTypeNotFoundException;
import com.clinic.mgmt.treatment.mapper.TreatmentTypeMapper;
import com.clinic.mgmt.treatment.repository.TreatmentTypeRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class TreatmentTypeService {

	private final TreatmentTypeRepository repository;
	private final TreatmentTypeMapper mapper;
	private final MongoTemplate mongoTemplate;
	private final AuditLogService auditLogService;

	public TreatmentTypeService(
			TreatmentTypeRepository repository,
			TreatmentTypeMapper mapper,
			MongoTemplate mongoTemplate,
			AuditLogService auditLogService
	) {
		this.repository = repository;
		this.mapper = mapper;
		this.mongoTemplate = mongoTemplate;
		this.auditLogService = auditLogService;
	}

	public List<TreatmentType> list(String organizationId, String clinicId, Boolean active) {
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Query query = new Query();
		if (scopedOrganizationId != null) {
			query.addCriteria(Criteria.where("organization_id").is(scopedOrganizationId));
		}
		if (clinicId != null) {
			query.addCriteria(Criteria.where("clinic_id").is(clinicId));
		}
		if (active != null) {
			query.addCriteria(Criteria.where("active").is(active));
		}
		return mongoTemplate.find(query, TreatmentType.class);
	}

	public TreatmentType get(String id, String organizationId) {
		TreatmentType type = repository.findById(id)
				.orElseThrow(() -> new TreatmentTypeNotFoundException("Treatment type not found"));
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		if (scopedOrganizationId != null && !scopedOrganizationId.equals(type.getOrganizationId())) {
			throw new TreatmentTypeNotFoundException("Treatment type not found");
		}
		return type;
	}

	public TreatmentType create(
			TreatmentTypeRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		Optional<TreatmentType> existing = repository.findByClinicIdAndName(
				request.getClinicId(),
				request.getName()
		);
		if (existing.isPresent()) {
			throw new DuplicateTreatmentTypeException("Treatment type already exists for clinic");
		}
		TreatmentType type = mapper.toNewEntity(request);
		type.setId(UUID.randomUUID().toString());
		type.setOrganizationId(resolvedOrganizationId);
		TreatmentType saved = repository.save(type);
		auditLogService.record(
				actorUserId,
				"create",
				"treatment_types",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public TreatmentType update(
			String id,
			TreatmentTypeRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		TreatmentType type = get(id, organizationId);
		Optional<TreatmentType> existing = repository.findByClinicIdAndName(
				request.getClinicId(),
				request.getName()
		);
		if (existing.isPresent() && !existing.get().getId().equals(type.getId())) {
			throw new DuplicateTreatmentTypeException("Treatment type already exists for clinic");
		}
		mapper.applyRequest(type, request, false);
		type.setOrganizationId(resolvedOrganizationId);
		TreatmentType saved = repository.save(type);
		auditLogService.record(
				actorUserId,
				"update",
				"treatment_types",
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
		TreatmentType type = get(id, organizationId);
		type.setActive(false);
		repository.save(type);
		auditLogService.record(
				actorUserId,
				"delete",
				"treatment_types",
				List.of(type.getId()),
				ipAddress,
				userAgent
		);
	}
}
