package com.clinic.mgmt.reference.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.common.exception.InvalidRequestException;
import com.clinic.mgmt.common.tenant.TenantGuard;
import com.clinic.mgmt.reference.domain.Branch;
import com.clinic.mgmt.reference.dto.BranchRequest;
import com.clinic.mgmt.reference.exception.BranchNotFoundException;
import com.clinic.mgmt.reference.exception.DuplicateBranchException;
import com.clinic.mgmt.reference.mapper.BranchMapper;
import com.clinic.mgmt.reference.repository.BranchRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class BranchService {

	private final BranchRepository repository;
	private final BranchMapper mapper;
	private final MongoTemplate mongoTemplate;
	private final AuditLogService auditLogService;

	public BranchService(
			BranchRepository repository,
			BranchMapper mapper,
			MongoTemplate mongoTemplate,
			AuditLogService auditLogService
	) {
		this.repository = repository;
		this.mapper = mapper;
		this.mongoTemplate = mongoTemplate;
		this.auditLogService = auditLogService;
	}

	public List<Branch> list(
			String requestOrganizationId,
			String actorOrganizationId,
			String clinicId,
			Boolean active
	) {
		String scopedOrganizationId = TenantGuard.resolveScope(requestOrganizationId, actorOrganizationId);
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
		return mongoTemplate.find(query, Branch.class);
	}

	public Branch get(String id, String organizationId) {
		Branch branch = repository.findById(id)
				.orElseThrow(() -> new BranchNotFoundException("Branch not found"));
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		if (scopedOrganizationId != null && !scopedOrganizationId.equals(branch.getOrganizationId())) {
			throw new BranchNotFoundException("Branch not found");
		}
		return branch;
	}

	public Branch create(
			BranchRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		ensureUnique(resolvedOrganizationId, request.getClinicId(), request.getCode(), null);
		Branch branch = mapper.toNewEntity(request);
		branch.setId(UUID.randomUUID().toString());
		branch.setOrganizationId(resolvedOrganizationId);
		Branch saved = repository.save(branch);
		auditLogService.record(
				actorUserId,
				"create",
				"branches",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public Branch update(
			String id,
			BranchRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		Branch branch = get(id, organizationId);
		if (!resolvedOrganizationId.equals(branch.getOrganizationId())) {
			throw new InvalidRequestException("organization_id cannot be changed for branches");
		}
		ensureUnique(resolvedOrganizationId, request.getClinicId(), request.getCode(), branch.getId());
		mapper.applyRequest(branch, request, false);
		branch.setOrganizationId(resolvedOrganizationId);
		Branch saved = repository.save(branch);
		auditLogService.record(
				actorUserId,
				"update",
				"branches",
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
		Branch branch = get(id, organizationId);
		branch.setActive(false);
		repository.save(branch);
		auditLogService.record(
				actorUserId,
				"delete",
				"branches",
				List.of(branch.getId()),
				ipAddress,
				userAgent
		);
	}

	private void ensureUnique(String organizationId, String clinicId, String code, String existingId) {
		Optional<Branch> byOrgCode = repository.findByOrganizationIdAndCode(organizationId, code);
		if (byOrgCode.isPresent() && !byOrgCode.get().getId().equals(existingId)) {
			throw new DuplicateBranchException("Branch code already exists for organization");
		}
		Optional<Branch> byClinicCode = repository.findByClinicIdAndCode(clinicId, code);
		if (byClinicCode.isPresent() && !byClinicCode.get().getId().equals(existingId)) {
			throw new DuplicateBranchException("Branch code already exists for clinic");
		}
	}
}
