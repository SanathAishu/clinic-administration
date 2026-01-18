package com.clinic.mgmt.visit.mapper;

import com.clinic.mgmt.common.exception.InvalidRequestException;
import com.clinic.mgmt.visit.domain.HouseVisit;
import com.clinic.mgmt.visit.domain.HouseVisitStatus;
import com.clinic.mgmt.visit.dto.HouseVisitRequest;
import com.clinic.mgmt.visit.dto.HouseVisitResponse;
import org.springframework.stereotype.Component;

@Component
public class HouseVisitMapper {

	public HouseVisit toNewEntity(HouseVisitRequest request) {
		HouseVisit visit = new HouseVisit();
		applyRequest(visit, request, true);
		return visit;
	}

	public void applyRequest(HouseVisit visit, HouseVisitRequest request, boolean isCreate) {
		visit.setOrganizationId(request.getOrganizationId());
		visit.setClinicId(request.getClinicId());
		visit.setBranchId(request.getBranchId());
		visit.setPatientId(request.getPatientId());
		visit.setProviderId(request.getProviderId());
		visit.setScheduledAt(request.getScheduledAt());
		visit.setAddress(request.getAddress());
		visit.setLatitude(request.getLatitude());
		visit.setLongitude(request.getLongitude());
		visit.setNotes(request.getNotes());
		if (request.getStatus() != null) {
			visit.setStatus(normalizeStatus(request.getStatus()));
		} else if (isCreate && visit.getStatus() == null) {
			visit.setStatus(HouseVisitStatus.SCHEDULED.getValue());
		}
	}

	public HouseVisitResponse toResponse(HouseVisit visit) {
		HouseVisitResponse response = new HouseVisitResponse();
		response.setId(visit.getId());
		response.setOrganizationId(visit.getOrganizationId());
		response.setClinicId(visit.getClinicId());
		response.setBranchId(visit.getBranchId());
		response.setPatientId(visit.getPatientId());
		response.setProviderId(visit.getProviderId());
		response.setScheduledAt(visit.getScheduledAt());
		response.setStatus(visit.getStatus());
		response.setAddress(visit.getAddress());
		response.setLatitude(visit.getLatitude());
		response.setLongitude(visit.getLongitude());
		response.setNotes(visit.getNotes());
		response.setCreatedAt(visit.getCreatedAt());
		response.setUpdatedAt(visit.getUpdatedAt());
		return response;
	}

	private String normalizeStatus(String status) {
		try {
			return HouseVisitStatus.fromValue(status).getValue();
		} catch (IllegalArgumentException ex) {
			throw new InvalidRequestException(ex.getMessage());
		}
	}
}
