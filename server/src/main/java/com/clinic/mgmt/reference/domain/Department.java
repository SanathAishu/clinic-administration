package com.clinic.mgmt.reference.domain;

import com.clinic.mgmt.common.domain.BaseEntity;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("departments")
@CompoundIndex(
		name = "uk_departments_clinic_code",
		def = "{'clinic_id': 1, 'code': 1}",
		unique = true
)
public class Department extends BaseEntity {

	@Field("organization_id")
	private String organizationId;

	@Field("clinic_id")
	private String clinicId;

	private String name;

	private String code;

	private Boolean active;

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public String getClinicId() {
		return clinicId;
	}

	public void setClinicId(String clinicId) {
		this.clinicId = clinicId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}
}
