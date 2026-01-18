package com.clinic.mgmt.treatment.service;

import java.util.List;
import java.util.UUID;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.common.tenant.TenantGuard;
import com.clinic.mgmt.common.exception.InvalidRequestException;
import com.clinic.mgmt.treatment.domain.Treatment;
import com.clinic.mgmt.treatment.domain.TreatmentStatus;
import com.clinic.mgmt.treatment.dto.TreatmentRequest;
import com.clinic.mgmt.treatment.exception.TreatmentNotFoundException;
import com.clinic.mgmt.treatment.mapper.TreatmentMapper;
import com.clinic.mgmt.treatment.repository.TreatmentRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class TreatmentService {

	private final TreatmentRepository repository;
	private final TreatmentMapper mapper;
	private final MongoTemplate mongoTemplate;
	private final AuditLogService auditLogService;

	public TreatmentService(
			TreatmentRepository repository,
			TreatmentMapper mapper,
			MongoTemplate mongoTemplate,
			AuditLogService auditLogService
	) {
		this.repository = repository;
		this.mapper = mapper;
		this.mongoTemplate = mongoTemplate;
		this.auditLogService = auditLogService;
	}

	public List<Treatment> list(
			String organizationId,
			String patientId,
			String status,
			String clinicId,
			String providerId
	) {
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Query query = new Query();
		if (scopedOrganizationId != null) {
			query.addCriteria(Criteria.where("organization_id").is(scopedOrganizationId));
		}
		if (patientId != null) {
			query.addCriteria(Criteria.where("patient_id").is(patientId));
		}
		if (status != null) {
			query.addCriteria(Criteria.where("status").is(normalizeStatus(status)));
		}
		if (clinicId != null) {
			query.addCriteria(Criteria.where("clinic_id").is(clinicId));
		}
		if (providerId != null) {
			query.addCriteria(Criteria.where("provider_id").is(providerId));
		}
		return mongoTemplate.find(query, Treatment.class);
	}

	public Treatment get(String id, String organizationId) {
		Treatment treatment = repository.findById(id)
				.orElseThrow(() -> new TreatmentNotFoundException("Treatment not found"));
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		if (scopedOrganizationId != null && !scopedOrganizationId.equals(treatment.getOrganizationId())) {
			throw new TreatmentNotFoundException("Treatment not found");
		}
		return treatment;
	}

	public Treatment create(
			TreatmentRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		Treatment treatment = mapper.toNewEntity(request);
		treatment.setId(UUID.randomUUID().toString());
		treatment.setOrganizationId(resolvedOrganizationId);
		Treatment saved = repository.save(treatment);
		auditLogService.record(
				actorUserId,
				"create",
				"treatments",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public Treatment update(
			String id,
			TreatmentRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		Treatment treatment = get(id, organizationId);
		mapper.applyRequest(treatment, request, false);
		treatment.setOrganizationId(resolvedOrganizationId);
		Treatment saved = repository.save(treatment);
		auditLogService.record(
				actorUserId,
				"update",
				"treatments",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public void cancel(String id, String actorUserId, String organizationId, String ipAddress, String userAgent) {
		Treatment treatment = get(id, organizationId);
		treatment.setStatus(TreatmentStatus.CANCELLED.getValue());
		repository.save(treatment);
		auditLogService.record(
				actorUserId,
				"delete",
				"treatments",
				List.of(treatment.getId()),
				ipAddress,
				userAgent
		);
	}

	private String normalizeStatus(String status) {
		try {
			return TreatmentStatus.fromValue(status).getValue();
		} catch (IllegalArgumentException ex) {
			throw new InvalidRequestException(ex.getMessage());
		}
	}
}
