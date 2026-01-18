package com.clinic.mgmt.audit.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.clinic.mgmt.audit.domain.AuditLog;
import com.clinic.mgmt.audit.repository.AuditLogRepository;
import com.clinic.mgmt.common.exception.InvalidRequestException;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

	private final AuditLogRepository repository;

	public AuditLogService(AuditLogRepository repository) {
		this.repository = repository;
	}

	public void record(String actorUserId, String action, String resource, Map<String, Object> payload) {
		record(actorUserId, action, resource, payload, null, null);
	}

	public void record(
			String actorUserId,
			String action,
			String resource,
			List<String> targetIds,
			String ipAddress,
			String userAgent
	) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("target_ids", targetIds == null ? List.of() : targetIds);
		record(actorUserId, action, resource, payload, ipAddress, userAgent);
	}

	private void record(
			String actorUserId,
			String action,
			String resource,
			Map<String, Object> payload,
			String ipAddress,
			String userAgent
	) {
		if (actorUserId == null || actorUserId.isBlank()) {
			throw new InvalidRequestException("actor_user_id is required for audit logging");
		}
		AuditLog log = new AuditLog();
		log.setId(UUID.randomUUID().toString());
		log.setUserId(actorUserId);
		log.setAction(action);
		log.setResource(resource);
		log.setPayload(buildPayload(actorUserId, action, resource, payload));
		log.setIpAddress(ipAddress);
		log.setUserAgent(userAgent);
		repository.save(log);
	}

	private Map<String, Object> buildPayload(
			String actorUserId,
			String action,
			String resource,
			Map<String, Object> payload
	) {
		Map<String, Object> enriched = new LinkedHashMap<>();
		enriched.put("actor_user_id", actorUserId);
		enriched.put("action", action);
		enriched.put("resource", resource);
		enriched.put("target_ids", extractTargetIds(payload));
		if (payload != null) {
			for (Map.Entry<String, Object> entry : payload.entrySet()) {
				if (!enriched.containsKey(entry.getKey())) {
					enriched.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return enriched;
	}

	private List<String> extractTargetIds(Map<String, Object> payload) {
		if (payload == null) {
			return List.of();
		}
		List<String> targetIds = toStringList(payload.get("target_ids"));
		if (!targetIds.isEmpty()) {
			return targetIds;
		}
		List<String> ids = toStringList(payload.get("ids"));
		if (!ids.isEmpty()) {
			return ids;
		}
		Object id = payload.get("id");
		if (id != null) {
			return List.of(id.toString());
		}
		return List.of();
	}

	private List<String> toStringList(Object value) {
		if (value == null) {
			return List.of();
		}
		if (value instanceof String stringValue) {
			return List.of(stringValue);
		}
		if (value instanceof Iterable<?> iterable) {
			List<String> items = new ArrayList<>();
			for (Object item : iterable) {
				if (item != null) {
					items.add(item.toString());
				}
			}
			return items;
		}
		return List.of();
	}
}
