package com.clinic.mgmt.visit.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.common.tenant.TenantGuard;
import com.clinic.mgmt.common.exception.InvalidRequestException;
import com.clinic.mgmt.visit.domain.HouseVisit;
import com.clinic.mgmt.visit.domain.HouseVisitStatus;
import com.clinic.mgmt.visit.dto.HouseVisitRequest;
import com.clinic.mgmt.visit.exception.HouseVisitNotFoundException;
import com.clinic.mgmt.visit.mapper.HouseVisitMapper;
import com.clinic.mgmt.visit.repository.HouseVisitRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class HouseVisitService {

	private final HouseVisitRepository repository;
	private final HouseVisitMapper mapper;
	private final MongoTemplate mongoTemplate;
	private final AuditLogService auditLogService;

	public HouseVisitService(
			HouseVisitRepository repository,
			HouseVisitMapper mapper,
			MongoTemplate mongoTemplate,
			AuditLogService auditLogService
	) {
		this.repository = repository;
		this.mapper = mapper;
		this.mongoTemplate = mongoTemplate;
		this.auditLogService = auditLogService;
	}

	public List<HouseVisit> list(
			String organizationId,
			String status,
			String providerId,
			Instant scheduledFrom,
			Instant scheduledTo
	) {
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Query query = new Query();
		if (scopedOrganizationId != null) {
			query.addCriteria(Criteria.where("organization_id").is(scopedOrganizationId));
		}
		if (status != null) {
			query.addCriteria(Criteria.where("status").is(normalizeStatus(status)));
		}
		if (providerId != null) {
			query.addCriteria(Criteria.where("provider_id").is(providerId));
		}
		if (scheduledFrom != null || scheduledTo != null) {
			Criteria dateCriteria = Criteria.where("scheduled_at");
			if (scheduledFrom != null) {
				dateCriteria = dateCriteria.gte(scheduledFrom);
			}
			if (scheduledTo != null) {
				dateCriteria = dateCriteria.lte(scheduledTo);
			}
			query.addCriteria(dateCriteria);
		}
		return mongoTemplate.find(query, HouseVisit.class);
	}

	public HouseVisit get(String id, String organizationId) {
		HouseVisit visit = repository.findById(id)
				.orElseThrow(() -> new HouseVisitNotFoundException("House visit not found"));
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		if (scopedOrganizationId != null && !scopedOrganizationId.equals(visit.getOrganizationId())) {
			throw new HouseVisitNotFoundException("House visit not found");
		}
		return visit;
	}

	public HouseVisit create(
			HouseVisitRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		HouseVisit visit = mapper.toNewEntity(request);
		visit.setId(UUID.randomUUID().toString());
		visit.setOrganizationId(resolvedOrganizationId);
		HouseVisit saved = repository.save(visit);
		auditLogService.record(
				actorUserId,
				"create",
				"house_visits",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public HouseVisit update(
			String id,
			HouseVisitRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		HouseVisit visit = get(id, organizationId);
		mapper.applyRequest(visit, request, false);
		visit.setOrganizationId(resolvedOrganizationId);
		HouseVisit saved = repository.save(visit);
		auditLogService.record(
				actorUserId,
				"update",
				"house_visits",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public void cancel(String id, String actorUserId, String organizationId, String ipAddress, String userAgent) {
		HouseVisit visit = get(id, organizationId);
		visit.setStatus(HouseVisitStatus.CANCELLED.getValue());
		repository.save(visit);
		auditLogService.record(
				actorUserId,
				"delete",
				"house_visits",
				List.of(visit.getId()),
				ipAddress,
				userAgent
		);
	}

	private String normalizeStatus(String status) {
		try {
			return HouseVisitStatus.fromValue(status).getValue();
		} catch (IllegalArgumentException ex) {
			throw new InvalidRequestException(ex.getMessage());
		}
	}
}
