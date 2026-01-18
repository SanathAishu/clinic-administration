package com.clinic.mgmt.treatment.mapper;

import com.clinic.mgmt.treatment.domain.TreatmentType;
import com.clinic.mgmt.treatment.dto.TreatmentTypeRequest;
import com.clinic.mgmt.treatment.dto.TreatmentTypeResponse;
import org.springframework.stereotype.Component;

@Component
public class TreatmentTypeMapper {

	private static final String CATEGORY_LOOKUP = "treatment_category";

	public TreatmentType toNewEntity(TreatmentTypeRequest request) {
		TreatmentType type = new TreatmentType();
		applyRequest(type, request, true);
		return type;
	}

	public void applyRequest(TreatmentType type, TreatmentTypeRequest request, boolean isCreate) {
		type.setOrganizationId(request.getOrganizationId());
		type.setClinicId(request.getClinicId());
		type.setName(request.getName());
		type.setCategoryLookupType(CATEGORY_LOOKUP);
		type.setCategoryCode(request.getCategoryCode());
		type.setDurationMinutes(request.getDurationMinutes());
		type.setBaseRate(request.getBaseRate());
		if (request.getActive() != null) {
			type.setActive(request.getActive());
		} else if (isCreate && type.getActive() == null) {
			type.setActive(true);
		}
	}

	public TreatmentTypeResponse toResponse(TreatmentType type) {
		TreatmentTypeResponse response = new TreatmentTypeResponse();
		response.setId(type.getId());
		response.setOrganizationId(type.getOrganizationId());
		response.setClinicId(type.getClinicId());
		response.setName(type.getName());
		response.setCategoryLookupType(type.getCategoryLookupType());
		response.setCategoryCode(type.getCategoryCode());
		response.setDurationMinutes(type.getDurationMinutes());
		response.setBaseRate(type.getBaseRate());
		response.setActive(type.getActive());
		response.setCreatedAt(type.getCreatedAt());
		response.setUpdatedAt(type.getUpdatedAt());
		return response;
	}
}
