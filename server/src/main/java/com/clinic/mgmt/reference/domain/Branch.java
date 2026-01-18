package com.clinic.mgmt.reference.domain;

import com.clinic.mgmt.common.domain.BaseEntity;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("branches")
@CompoundIndex(
		name = "uk_branches_org_code",
		def = "{'organization_id': 1, 'code': 1}",
		unique = true
)
@CompoundIndex(
		name = "uk_branches_clinic_code",
		def = "{'clinic_id': 1, 'code': 1}",
		unique = true
)
public class Branch extends BaseEntity {

	@Field("organization_id")
	private String organizationId;

	@Field("clinic_id")
	private String clinicId;

	private String name;

	private String code;

	@Field("address_line1")
	private String addressLine1;

	@Field("address_line2")
	private String addressLine2;

	private String city;

	private String state;

	@Field("postal_code")
	private String postalCode;

	private String phone;

	private String email;

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

	public String getAddressLine1() {
		return addressLine1;
	}

	public void setAddressLine1(String addressLine1) {
		this.addressLine1 = addressLine1;
	}

	public String getAddressLine2() {
		return addressLine2;
	}

	public void setAddressLine2(String addressLine2) {
		this.addressLine2 = addressLine2;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}
}
