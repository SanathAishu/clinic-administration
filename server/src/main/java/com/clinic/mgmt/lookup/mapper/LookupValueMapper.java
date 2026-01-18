package com.clinic.mgmt.lookup.mapper;

import com.clinic.mgmt.lookup.domain.LookupValue;
import com.clinic.mgmt.lookup.dto.LookupValueRequest;
import com.clinic.mgmt.lookup.dto.LookupValueResponse;
import org.springframework.stereotype.Component;

@Component
public class LookupValueMapper {

	public LookupValue toNewEntity(LookupValueRequest request) {
		LookupValue value = new LookupValue();
		applyRequest(value, request, true);
		return value;
	}

	public void applyRequest(LookupValue value, LookupValueRequest request, boolean isCreate) {
		value.setLookupType(request.getLookupType());
		value.setCode(request.getCode());
		value.setLabel(request.getLabel());
		value.setSortOrder(request.getSortOrder());
		if (request.getActive() != null) {
			value.setActive(request.getActive());
		} else if (isCreate && value.getActive() == null) {
			value.setActive(true);
		}
		value.setMeta(request.getMeta());
	}

	public LookupValueResponse toResponse(LookupValue value) {
		LookupValueResponse response = new LookupValueResponse();
		response.setId(value.getId());
		response.setLookupType(value.getLookupType());
		response.setCode(value.getCode());
		response.setLabel(value.getLabel());
		response.setSortOrder(value.getSortOrder());
		response.setActive(value.getActive());
		response.setMeta(value.getMeta());
		response.setCreatedAt(value.getCreatedAt());
		response.setUpdatedAt(value.getUpdatedAt());
		return response;
	}
}
