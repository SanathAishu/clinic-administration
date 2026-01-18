package com.clinic.mgmt.appointment.mapper;

import com.clinic.mgmt.appointment.domain.Appointment;
import com.clinic.mgmt.appointment.domain.AppointmentStatus;
import com.clinic.mgmt.appointment.dto.AppointmentRequest;
import com.clinic.mgmt.appointment.dto.AppointmentResponse;
import com.clinic.mgmt.common.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

	public Appointment toNewEntity(AppointmentRequest request) {
		Appointment appointment = new Appointment();
		applyRequest(appointment, request, true);
		return appointment;
	}

	public void applyRequest(Appointment appointment, AppointmentRequest request, boolean isCreate) {
		appointment.setOrganizationId(request.getOrganizationId());
		appointment.setClinicId(request.getClinicId());
		appointment.setBranchId(request.getBranchId());
		appointment.setPatientId(request.getPatientId());
		appointment.setProviderId(request.getProviderId());
		appointment.setDepartmentId(request.getDepartmentId());
		appointment.setScheduledStart(request.getScheduledStart());
		appointment.setScheduledEnd(request.getScheduledEnd());
		appointment.setReason(request.getReason());
		appointment.setNotes(request.getNotes());
		if (request.getStatus() != null) {
			appointment.setStatus(normalizeStatus(request.getStatus()));
		} else if (isCreate && appointment.getStatus() == null) {
			appointment.setStatus(AppointmentStatus.SCHEDULED.getValue());
		}
	}

	public AppointmentResponse toResponse(Appointment appointment) {
		AppointmentResponse response = new AppointmentResponse();
		response.setId(appointment.getId());
		response.setOrganizationId(appointment.getOrganizationId());
		response.setClinicId(appointment.getClinicId());
		response.setBranchId(appointment.getBranchId());
		response.setPatientId(appointment.getPatientId());
		response.setProviderId(appointment.getProviderId());
		response.setDepartmentId(appointment.getDepartmentId());
		response.setScheduledStart(appointment.getScheduledStart());
		response.setScheduledEnd(appointment.getScheduledEnd());
		response.setStatus(appointment.getStatus());
		response.setReason(appointment.getReason());
		response.setNotes(appointment.getNotes());
		response.setCreatedBy(appointment.getCreatedBy());
		response.setCreatedAt(appointment.getCreatedAt());
		response.setUpdatedAt(appointment.getUpdatedAt());
		return response;
	}

	private String normalizeStatus(String status) {
		try {
			return AppointmentStatus.fromValue(status).getValue();
		} catch (IllegalArgumentException ex) {
			throw new InvalidRequestException(ex.getMessage());
		}
	}
}
