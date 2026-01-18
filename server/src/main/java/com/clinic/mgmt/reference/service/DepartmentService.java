package com.clinic.mgmt.reference.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.common.exception.InvalidRequestException;
import com.clinic.mgmt.common.tenant.TenantGuard;
import com.clinic.mgmt.reference.domain.Department;
import com.clinic.mgmt.reference.dto.DepartmentRequest;
import com.clinic.mgmt.reference.exception.DepartmentNotFoundException;
import com.clinic.mgmt.reference.exception.DuplicateDepartmentException;
import com.clinic.mgmt.reference.mapper.DepartmentMapper;
import com.clinic.mgmt.reference.repository.DepartmentRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class DepartmentService {

	private final DepartmentRepository repository;
	private final DepartmentMapper mapper;
	private final MongoTemplate mongoTemplate;
	private final AuditLogService auditLogService;

	public DepartmentService(
			DepartmentRepository repository,
			DepartmentMapper mapper,
			MongoTemplate mongoTemplate,
			AuditLogService auditLogService
	) {
		this.repository = repository;
		this.mapper = mapper;
		this.mongoTemplate = mongoTemplate;
		this.auditLogService = auditLogService;
	}

	public List<Department> list(
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
		return mongoTemplate.find(query, Department.class);
	}

	public Department get(String id, String organizationId) {
		Department department = repository.findById(id)
				.orElseThrow(() -> new DepartmentNotFoundException("Department not found"));
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		if (scopedOrganizationId != null && !scopedOrganizationId.equals(department.getOrganizationId())) {
			throw new DepartmentNotFoundException("Department not found");
		}
		return department;
	}

	public Department create(
			DepartmentRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		Optional<Department> existing = repository.findByClinicIdAndCode(
				request.getClinicId(),
				request.getCode()
		);
		if (existing.isPresent()) {
			throw new DuplicateDepartmentException("Department code already exists for clinic");
		}
		Department department = mapper.toNewEntity(request);
		department.setId(UUID.randomUUID().toString());
		department.setOrganizationId(resolvedOrganizationId);
		Department saved = repository.save(department);
		auditLogService.record(
				actorUserId,
				"create",
				"departments",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public Department update(
			String id,
			DepartmentRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		Department department = get(id, organizationId);
		if (!resolvedOrganizationId.equals(department.getOrganizationId())) {
			throw new InvalidRequestException("organization_id cannot be changed for departments");
		}
		Optional<Department> existing = repository.findByClinicIdAndCode(
				request.getClinicId(),
				request.getCode()
		);
		if (existing.isPresent() && !existing.get().getId().equals(department.getId())) {
			throw new DuplicateDepartmentException("Department code already exists for clinic");
		}
		mapper.applyRequest(department, request, false);
		department.setOrganizationId(resolvedOrganizationId);
		Department saved = repository.save(department);
		auditLogService.record(
				actorUserId,
				"update",
				"departments",
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
		Department department = get(id, organizationId);
		department.setActive(false);
		repository.save(department);
		auditLogService.record(
				actorUserId,
				"delete",
				"departments",
				List.of(department.getId()),
				ipAddress,
				userAgent
		);
	}
}
