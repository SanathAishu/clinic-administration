package com.clinic.mgmt.patient.mapper;

import com.clinic.mgmt.common.exception.InvalidRequestException;
import com.clinic.mgmt.patient.domain.Patient;
import com.clinic.mgmt.patient.domain.PatientStatus;
import com.clinic.mgmt.patient.dto.PatientRequest;
import com.clinic.mgmt.patient.dto.PatientResponse;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

	private static final String GENDER_LOOKUP = "gender";

	public Patient toNewEntity(PatientRequest request) {
		Patient patient = new Patient();
		applyRequest(patient, request, true);
		return patient;
	}

	public void applyRequest(Patient patient, PatientRequest request, boolean isCreate) {
		patient.setOrganizationId(request.getOrganizationId());
		patient.setClinicId(request.getClinicId());
		patient.setBranchId(request.getBranchId());
		patient.setFirstName(request.getFirstName());
		patient.setLastName(request.getLastName());
		patient.setFullName(buildFullName(request.getFirstName(), request.getLastName()));
		patient.setGenderLookupType(GENDER_LOOKUP);
		patient.setGenderCode(request.getGenderCode());
		patient.setDateOfBirth(request.getDateOfBirth());
		patient.setPhone(request.getPhone());
		patient.setEmail(request.getEmail());
		patient.setAddressLine1(request.getAddressLine1());
		patient.setAddressLine2(request.getAddressLine2());
		patient.setCity(request.getCity());
		patient.setState(request.getState());
		patient.setPostalCode(request.getPostalCode());
		patient.setEmergencyContactName(request.getEmergencyContactName());
		patient.setEmergencyContactPhone(request.getEmergencyContactPhone());
		String status = request.getStatus();
		if (status != null && !status.isBlank()) {
			patient.setStatus(normalizeStatus(status));
		} else if (isCreate && patient.getStatus() == null) {
			patient.setStatus(PatientStatus.ACTIVE.getValue());
		}
	}

	public PatientResponse toResponse(Patient patient) {
		PatientResponse response = new PatientResponse();
		response.setId(patient.getId());
		response.setOrganizationId(patient.getOrganizationId());
		response.setClinicId(patient.getClinicId());
		response.setBranchId(patient.getBranchId());
		response.setFirstName(patient.getFirstName());
		response.setLastName(patient.getLastName());
		response.setFullName(patient.getFullName());
		response.setGenderLookupType(patient.getGenderLookupType());
		response.setGenderCode(patient.getGenderCode());
		response.setDateOfBirth(patient.getDateOfBirth());
		response.setPhone(patient.getPhone());
		response.setEmail(patient.getEmail());
		response.setAddressLine1(patient.getAddressLine1());
		response.setAddressLine2(patient.getAddressLine2());
		response.setCity(patient.getCity());
		response.setState(patient.getState());
		response.setPostalCode(patient.getPostalCode());
		response.setEmergencyContactName(patient.getEmergencyContactName());
		response.setEmergencyContactPhone(patient.getEmergencyContactPhone());
		response.setStatus(patient.getStatus());
		response.setCreatedAt(patient.getCreatedAt());
		response.setUpdatedAt(patient.getUpdatedAt());
		return response;
	}

	private String buildFullName(String firstName, String lastName) {
		String safeFirst = firstName == null ? "" : firstName.trim();
		String safeLast = lastName == null ? "" : lastName.trim();
		if (safeLast.isEmpty()) {
			return safeFirst;
		}
		return (safeFirst + " " + safeLast).trim();
	}

	private String normalizeStatus(String status) {
		try {
			return PatientStatus.fromValue(status).getValue();
		} catch (IllegalArgumentException ex) {
			throw new InvalidRequestException(ex.getMessage());
		}
	}
}
