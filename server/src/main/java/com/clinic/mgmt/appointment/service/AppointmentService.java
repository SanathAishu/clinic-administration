package com.clinic.mgmt.appointment.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.clinic.mgmt.appointment.domain.Appointment;
import com.clinic.mgmt.appointment.domain.AppointmentStatus;
import com.clinic.mgmt.appointment.dto.AppointmentRequest;
import com.clinic.mgmt.appointment.exception.AppointmentNotFoundException;
import com.clinic.mgmt.appointment.mapper.AppointmentMapper;
import com.clinic.mgmt.appointment.repository.AppointmentRepository;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.common.tenant.TenantGuard;
import com.clinic.mgmt.common.exception.InvalidRequestException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class AppointmentService {

	private final AppointmentRepository repository;
	private final AppointmentMapper mapper;
	private final MongoTemplate mongoTemplate;
	private final AuditLogService auditLogService;

	public AppointmentService(
			AppointmentRepository repository,
			AppointmentMapper mapper,
			MongoTemplate mongoTemplate,
			AuditLogService auditLogService
	) {
		this.repository = repository;
		this.mapper = mapper;
		this.mongoTemplate = mongoTemplate;
		this.auditLogService = auditLogService;
	}

	public List<Appointment> list(
			String organizationId,
			String status,
			Instant scheduledFrom,
			Instant scheduledTo,
			String clinicId,
			String branchId,
			String patientId,
			String providerId
	) {
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		Query query = new Query();
		if (scopedOrganizationId != null) {
			query.addCriteria(Criteria.where("organization_id").is(scopedOrganizationId));
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
		if (patientId != null) {
			query.addCriteria(Criteria.where("patient_id").is(patientId));
		}
		if (providerId != null) {
			query.addCriteria(Criteria.where("provider_id").is(providerId));
		}
		if (scheduledFrom != null || scheduledTo != null) {
			Criteria dateCriteria = Criteria.where("scheduled_start");
			if (scheduledFrom != null) {
				dateCriteria = dateCriteria.gte(scheduledFrom);
			}
			if (scheduledTo != null) {
				dateCriteria = dateCriteria.lte(scheduledTo);
			}
			query.addCriteria(dateCriteria);
		}
		return mongoTemplate.find(query, Appointment.class);
	}

	public Appointment get(String id, String organizationId) {
		Appointment appointment = repository.findById(id)
				.orElseThrow(() -> new AppointmentNotFoundException("Appointment not found"));
		String scopedOrganizationId = TenantGuard.resolveScope(null, organizationId);
		if (scopedOrganizationId != null && !scopedOrganizationId.equals(appointment.getOrganizationId())) {
			throw new AppointmentNotFoundException("Appointment not found");
		}
		return appointment;
	}

	public Appointment create(
			AppointmentRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		validateSchedule(request.getScheduledStart(), request.getScheduledEnd());
		Appointment appointment = mapper.toNewEntity(request);
		appointment.setId(UUID.randomUUID().toString());
		appointment.setOrganizationId(resolvedOrganizationId);
		appointment.setCreatedBy(actorUserId);
		Appointment saved = repository.save(appointment);
		auditLogService.record(
				actorUserId,
				"create",
				"appointments",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public Appointment update(
			String id,
			AppointmentRequest request,
			String actorUserId,
			String organizationId,
			String ipAddress,
			String userAgent
	) {
		String resolvedOrganizationId = TenantGuard.resolveOrganizationId(
				request.getOrganizationId(),
				organizationId
		);
		Appointment appointment = get(id, organizationId);
		validateSchedule(request.getScheduledStart(), request.getScheduledEnd());
		mapper.applyRequest(appointment, request, false);
		appointment.setOrganizationId(resolvedOrganizationId);
		Appointment saved = repository.save(appointment);
		auditLogService.record(
				actorUserId,
				"update",
				"appointments",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public void cancel(String id, String actorUserId, String organizationId, String ipAddress, String userAgent) {
		Appointment appointment = get(id, organizationId);
		appointment.setStatus(AppointmentStatus.CANCELLED.getValue());
		repository.save(appointment);
		auditLogService.record(
				actorUserId,
				"delete",
				"appointments",
				List.of(appointment.getId()),
				ipAddress,
				userAgent
		);
	}

	private String normalizeStatus(String status) {
		try {
			return AppointmentStatus.fromValue(status).getValue();
		} catch (IllegalArgumentException ex) {
			throw new InvalidRequestException(ex.getMessage());
		}
	}

	private void validateSchedule(Instant start, Instant end) {
		if (start != null && end != null && !end.isAfter(start)) {
			throw new InvalidRequestException("scheduled_end must be after scheduled_start");
		}
	}
}
