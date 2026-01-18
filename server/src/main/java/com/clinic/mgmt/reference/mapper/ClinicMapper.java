package com.clinic.mgmt.reference.mapper;

import com.clinic.mgmt.reference.domain.Clinic;
import com.clinic.mgmt.reference.dto.ClinicRequest;
import com.clinic.mgmt.reference.dto.ClinicResponse;
import org.springframework.stereotype.Component;

@Component
public class ClinicMapper {

	public Clinic toNewEntity(ClinicRequest request) {
		Clinic clinic = new Clinic();
		applyRequest(clinic, request, true);
		return clinic;
	}

	public void applyRequest(Clinic clinic, ClinicRequest request, boolean isCreate) {
		clinic.setOrganizationId(request.getOrganizationId());
		clinic.setName(request.getName());
		clinic.setCode(request.getCode());
		clinic.setAddressLine1(request.getAddressLine1());
		clinic.setAddressLine2(request.getAddressLine2());
		clinic.setCity(request.getCity());
		clinic.setState(request.getState());
		clinic.setPostalCode(request.getPostalCode());
		clinic.setPhone(request.getPhone());
		clinic.setEmail(request.getEmail());
		clinic.setTimezone(request.getTimezone());
		if (request.getActive() != null) {
			clinic.setActive(request.getActive());
		} else if (isCreate && clinic.getActive() == null) {
			clinic.setActive(true);
		}
	}

	public ClinicResponse toResponse(Clinic clinic) {
		ClinicResponse response = new ClinicResponse();
		response.setId(clinic.getId());
		response.setOrganizationId(clinic.getOrganizationId());
		response.setName(clinic.getName());
		response.setCode(clinic.getCode());
		response.setAddressLine1(clinic.getAddressLine1());
		response.setAddressLine2(clinic.getAddressLine2());
		response.setCity(clinic.getCity());
		response.setState(clinic.getState());
		response.setPostalCode(clinic.getPostalCode());
		response.setPhone(clinic.getPhone());
		response.setEmail(clinic.getEmail());
		response.setTimezone(clinic.getTimezone());
		response.setActive(clinic.getActive());
		response.setCreatedAt(clinic.getCreatedAt());
		response.setUpdatedAt(clinic.getUpdatedAt());
		return response;
	}
}
