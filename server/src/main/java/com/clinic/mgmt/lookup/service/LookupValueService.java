package com.clinic.mgmt.lookup.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.clinic.mgmt.audit.service.AuditLogService;
import com.clinic.mgmt.lookup.domain.LookupValue;
import com.clinic.mgmt.lookup.dto.LookupValueRequest;
import com.clinic.mgmt.lookup.exception.DuplicateLookupValueException;
import com.clinic.mgmt.lookup.exception.LookupValueNotFoundException;
import com.clinic.mgmt.lookup.mapper.LookupValueMapper;
import com.clinic.mgmt.lookup.repository.LookupValueRepository;
import org.springframework.stereotype.Service;

@Service
public class LookupValueService {

	private final LookupValueRepository repository;
	private final LookupValueMapper mapper;
	private final AuditLogService auditLogService;

	public LookupValueService(
			LookupValueRepository repository,
			LookupValueMapper mapper,
			AuditLogService auditLogService
	) {
		this.repository = repository;
		this.mapper = mapper;
		this.auditLogService = auditLogService;
	}

	public List<LookupValue> list(String lookupType, Boolean active) {
		if (lookupType != null && active != null) {
			return repository.findByLookupTypeAndActive(lookupType, active);
		}
		if (lookupType != null) {
			return repository.findByLookupType(lookupType);
		}
		if (active != null) {
			return repository.findByActive(active);
		}
		return repository.findAll();
	}

	public LookupValue get(String id) {
		return repository.findById(id)
				.orElseThrow(() -> new LookupValueNotFoundException("Lookup value not found"));
	}

	public LookupValue create(
			LookupValueRequest request,
			String actorUserId,
			String ipAddress,
			String userAgent
	) {
		Optional<LookupValue> existing = repository.findByLookupTypeAndCode(
				request.getLookupType(),
				request.getCode()
		);
		if (existing.isPresent()) {
			throw new DuplicateLookupValueException("Lookup value already exists");
		}
		LookupValue value = mapper.toNewEntity(request);
		value.setId(UUID.randomUUID().toString());
		LookupValue saved = repository.save(value);
		auditLogService.record(
				actorUserId,
				"create",
				"lookup_values",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public LookupValue update(
			String id,
			LookupValueRequest request,
			String actorUserId,
			String ipAddress,
			String userAgent
	) {
		LookupValue value = get(id);
		Optional<LookupValue> existing = repository.findByLookupTypeAndCode(
				request.getLookupType(),
				request.getCode()
		);
		if (existing.isPresent() && !existing.get().getId().equals(value.getId())) {
			throw new DuplicateLookupValueException("Lookup value already exists");
		}
		mapper.applyRequest(value, request, false);
		LookupValue saved = repository.save(value);
		auditLogService.record(
				actorUserId,
				"update",
				"lookup_values",
				List.of(saved.getId()),
				ipAddress,
				userAgent
		);
		return saved;
	}

	public void deactivate(String id, String actorUserId, String ipAddress, String userAgent) {
		LookupValue value = get(id);
		value.setActive(false);
		repository.save(value);
		auditLogService.record(
				actorUserId,
				"delete",
				"lookup_values",
				List.of(value.getId()),
				ipAddress,
				userAgent
		);
	}
}
