package com.clinic.mgmt.common.mongo;

import java.util.UUID;
import com.clinic.mgmt.common.domain.UuidIdentifiable;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

@Component
public class UuidBeforeConvertCallback implements BeforeConvertCallback<Object> {

	@Override
	public Object onBeforeConvert(Object entity, String collection) {
		if (entity instanceof UuidIdentifiable identifiable) {
			String id = identifiable.getId();
			if (id == null || id.isBlank()) {
				identifiable.setId(UUID.randomUUID().toString());
			}
		}
		return entity;
	}
}
