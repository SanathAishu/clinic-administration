package com.clinic.mgmt.common.organization.mapper;

import com.clinic.mgmt.common.organization.domain.Organization;
import com.clinic.mgmt.common.organization.dto.OrganizationRequest;
import com.clinic.mgmt.common.organization.dto.OrganizationResponse;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {

	public Organization toNewEntity(OrganizationRequest request) {
		Organization organization = new Organization();
		applyRequest(organization, request, true);
		return organization;
	}

	public void applyRequest(Organization organization, OrganizationRequest request, boolean isCreate) {
		organization.setName(request.getName());
		organization.setCode(request.getCode());
		if (request.getActive() != null) {
			organization.setActive(request.getActive());
		} else if (isCreate && organization.getActive() == null) {
			organization.setActive(true);
		}
	}

	public OrganizationResponse toResponse(Organization organization) {
		OrganizationResponse response = new OrganizationResponse();
		response.setId(organization.getId());
		response.setName(organization.getName());
		response.setCode(organization.getCode());
		response.setActive(organization.getActive());
		response.setCreatedAt(organization.getCreatedAt());
		response.setUpdatedAt(organization.getUpdatedAt());
		return response;
	}
}
