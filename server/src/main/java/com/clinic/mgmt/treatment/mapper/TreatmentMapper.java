package com.clinic.mgmt.treatment.mapper;

import com.clinic.mgmt.common.exception.InvalidRequestException;
import com.clinic.mgmt.treatment.domain.Treatment;
import com.clinic.mgmt.treatment.domain.TreatmentStatus;
import com.clinic.mgmt.treatment.dto.TreatmentRequest;
import com.clinic.mgmt.treatment.dto.TreatmentResponse;
import org.springframework.stereotype.Component;

@Component
public class TreatmentMapper {

	public Treatment toNewEntity(TreatmentRequest request) {
		Treatment treatment = new Treatment();
		applyRequest(treatment, request, true);
		return treatment;
	}

	public void applyRequest(Treatment treatment, TreatmentRequest request, boolean isCreate) {
		treatment.setOrganizationId(request.getOrganizationId());
		treatment.setClinicId(request.getClinicId());
		treatment.setBranchId(request.getBranchId());
		treatment.setPatientId(request.getPatientId());
		treatment.setAppointmentId(request.getAppointmentId());
		treatment.setTreatmentTypeId(request.getTreatmentTypeId());
		treatment.setProviderId(request.getProviderId());
		treatment.setNotes(request.getNotes());
		if (request.getStatus() != null) {
			treatment.setStatus(normalizeStatus(request.getStatus()));
		} else if (isCreate && treatment.getStatus() == null) {
			treatment.setStatus(TreatmentStatus.PLANNED.getValue());
		}
	}

	public TreatmentResponse toResponse(Treatment treatment) {
		TreatmentResponse response = new TreatmentResponse();
		response.setId(treatment.getId());
		response.setOrganizationId(treatment.getOrganizationId());
		response.setClinicId(treatment.getClinicId());
		response.setBranchId(treatment.getBranchId());
		response.setPatientId(treatment.getPatientId());
		response.setAppointmentId(treatment.getAppointmentId());
		response.setTreatmentTypeId(treatment.getTreatmentTypeId());
		response.setProviderId(treatment.getProviderId());
		response.setStatus(treatment.getStatus());
		response.setNotes(treatment.getNotes());
		response.setCreatedAt(treatment.getCreatedAt());
		response.setUpdatedAt(treatment.getUpdatedAt());
		return response;
	}

	private String normalizeStatus(String status) {
		try {
			return TreatmentStatus.fromValue(status).getValue();
		} catch (IllegalArgumentException ex) {
			throw new InvalidRequestException(ex.getMessage());
		}
	}
}
