package com.clinic.mgmt.reference.mapper;

import com.clinic.mgmt.reference.domain.Department;
import com.clinic.mgmt.reference.dto.DepartmentRequest;
import com.clinic.mgmt.reference.dto.DepartmentResponse;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

	public Department toNewEntity(DepartmentRequest request) {
		Department department = new Department();
		applyRequest(department, request, true);
		return department;
	}

	public void applyRequest(Department department, DepartmentRequest request, boolean isCreate) {
		department.setOrganizationId(request.getOrganizationId());
		department.setClinicId(request.getClinicId());
		department.setName(request.getName());
		department.setCode(request.getCode());
		if (request.getActive() != null) {
			department.setActive(request.getActive());
		} else if (isCreate && department.getActive() == null) {
			department.setActive(true);
		}
	}

	public DepartmentResponse toResponse(Department department) {
		DepartmentResponse response = new DepartmentResponse();
		response.setId(department.getId());
		response.setOrganizationId(department.getOrganizationId());
		response.setClinicId(department.getClinicId());
		response.setName(department.getName());
		response.setCode(department.getCode());
		response.setActive(department.getActive());
		response.setCreatedAt(department.getCreatedAt());
		response.setUpdatedAt(department.getUpdatedAt());
		return response;
	}
}
