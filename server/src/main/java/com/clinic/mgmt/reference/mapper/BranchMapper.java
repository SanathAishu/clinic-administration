package com.clinic.mgmt.reference.mapper;

import com.clinic.mgmt.reference.domain.Branch;
import com.clinic.mgmt.reference.dto.BranchRequest;
import com.clinic.mgmt.reference.dto.BranchResponse;
import org.springframework.stereotype.Component;

@Component
public class BranchMapper {

	public Branch toNewEntity(BranchRequest request) {
		Branch branch = new Branch();
		applyRequest(branch, request, true);
		return branch;
	}

	public void applyRequest(Branch branch, BranchRequest request, boolean isCreate) {
		branch.setOrganizationId(request.getOrganizationId());
		branch.setClinicId(request.getClinicId());
		branch.setName(request.getName());
		branch.setCode(request.getCode());
		branch.setAddressLine1(request.getAddressLine1());
		branch.setAddressLine2(request.getAddressLine2());
		branch.setCity(request.getCity());
		branch.setState(request.getState());
		branch.setPostalCode(request.getPostalCode());
		branch.setPhone(request.getPhone());
		branch.setEmail(request.getEmail());
		if (request.getActive() != null) {
			branch.setActive(request.getActive());
		} else if (isCreate && branch.getActive() == null) {
			branch.setActive(true);
		}
	}

	public BranchResponse toResponse(Branch branch) {
		BranchResponse response = new BranchResponse();
		response.setId(branch.getId());
		response.setOrganizationId(branch.getOrganizationId());
		response.setClinicId(branch.getClinicId());
		response.setName(branch.getName());
		response.setCode(branch.getCode());
		response.setAddressLine1(branch.getAddressLine1());
		response.setAddressLine2(branch.getAddressLine2());
		response.setCity(branch.getCity());
		response.setState(branch.getState());
		response.setPostalCode(branch.getPostalCode());
		response.setPhone(branch.getPhone());
		response.setEmail(branch.getEmail());
		response.setActive(branch.getActive());
		response.setCreatedAt(branch.getCreatedAt());
		response.setUpdatedAt(branch.getUpdatedAt());
		return response;
	}
}
