package com.clinic.mgmt.patient.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.common.tenant.TenantGuard;
import com.clinic.mgmt.common.exception.InvalidRequestException;
import com.clinic.mgmt.patient.domain.Patient;
import com.clinic.mgmt.patient.domain.PatientStatus;
import com.clinic.mgmt.patient.dto.PatientRequest;
import com.clinic.mgmt.patient.exception.DuplicatePatientException;
import com.clinic.mgmt.patient.exception.PatientNotFoundException;
import com.clinic.mgmt.patient.mapper.PatientMapper;
import com.clinic.mgmt.patient.repository.PatientRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

	private final PatientRepository repository;
	private final PatientMapper mapper;
	private final MongoTemplate mongoTemplate;
	private final AuditLogService auditLogService;

	public PatientService(
			PatientRepository repository,
			PatientMapper mapper,
			MongoTemplate mongoTemplate,
			AuditLogService auditLogService
	) {
		this.repository = repository;
		this.mapper = mapper;
		this.mongoTemplate = mongoTemplate;
		this.auditLogService = auditLogService;
	}

	public List<Patient> list(
			String organizationId,
			String phone,
			String name,
			String status,
			String clinicId,
			String branchId,
			Instant dateFrom,
			Instant dateTo
	) {
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Query query = new Query();
		if (scopedOrganizationId != null) {
			query.addCriteria(Criteria.where("organization_id").is(scopedOrganizationId));
		}
		if (phone != null) {
			query.addCriteria(Criteria.where("phone").is(phone));
		}
		if (name != null) {
			Pattern pattern = Pattern.compile(Pattern.quote(name), Pattern.CASE_INSENSITIVE);
			query.addCriteria(Criteria.where("full_name").regex(pattern));
		}
		if (status != null) {
			query.addCriteria(Criteria.where("status").is(normalizeStatus(status)));
		}
		if (clinicId != null) {
			query.addCriteria(Criteria.where("clinic_id").is(clinicId));
		}
		if (branchId != null) {
			query.addCriteria(Criteria.where("branch_id").is(branchId));
		}
		if (dateFrom != null || dateTo != null) {
			Criteria dateCriteria = Criteria.where("created_at");
			if (dateFrom != null) {
				dateCriteria = dateCriteria.gte(dateFrom);
			}
			if (dateTo != null) {
				dateCriteria = dateCriteria.lte(dateTo);
			}
			query.addCriteria(dateCriteria);
		}
		return mongoTemplate.find(query, Patient.class);
	}

	public Patient get(String id, String organizationId) {
		Patient patient = repository.findById(id)
				.orElseThrow(() -> new PatientNotFoundException("Patient not found"));
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		if (scopedOrganizationId != null && !scopedOrganizationId.equals(patient.getOrganizationId())) {
			throw new PatientNotFoundException("Patient not found");
		}
		return patient;
	}

	public Patient create(
			PatientRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		Optional<Patient> existing = repository.findByClinicIdAndPhoneAndStatusNot(
				request.getClinicId(),
				request.getPhone(),
				PatientStatus.ARCHIVED.getValue()
		);
		if (existing.isPresent()) {
			throw new DuplicatePatientException("Patient already exists for clinic and phone");
		}
		Patient patient = mapper.toNewEntity(request);
		patient.setId(UUID.randomUUID().toString());
		patient.setOrganizationId(resolvedOrganizationId);
		Patient saved = repository.save(patient);
		auditLogService.record(
				actorUserId,
				"create",
				"patients",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public Patient update(
			String id,
			PatientRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		Patient patient = get(id, organizationId);
		Optional<Patient> existing = repository.findByClinicIdAndPhoneAndStatusNot(
				request.getClinicId(),
				request.getPhone(),
				PatientStatus.ARCHIVED.getValue()
		);
		if (existing.isPresent() && !existing.get().getId().equals(patient.getId())) {
			throw new DuplicatePatientException("Patient already exists for clinic and phone");
		}
		mapper.applyRequest(patient, request, false);
		patient.setOrganizationId(resolvedOrganizationId);
		Patient saved = repository.save(patient);
		auditLogService.record(
				actorUserId,
				"update",
				"patients",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public void archive(
			String id,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		Patient patient = get(id, organizationId);
		patient.setStatus(PatientStatus.ARCHIVED.getValue());
		repository.save(patient);
		auditLogService.record(
				actorUserId,
				"delete",
				"patients",
				List.of(patient.getId()),
				ipAddress,
				userAgent
		);
	}

	private String normalizeStatus(String status) {
		try {
			return PatientStatus.fromValue(status).getValue();
		} catch (IllegalArgumentException ex) {
			throw new InvalidRequestException(ex.getMessage());
		}
	}
}
