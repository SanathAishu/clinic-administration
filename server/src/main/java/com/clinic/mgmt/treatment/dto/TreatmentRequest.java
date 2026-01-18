package com.clinic.mgmt.treatment.dto;

import jakarta.validation.constraints.NotBlank;

public class TreatmentRequest {

	@NotBlank
	private String organizationId;

	@NotBlank
	private String clinicId;

	@NotBlank
	private String branchId;

	@NotBlank
	private String patientId;

	private String appointmentId;

	@NotBlank
	private String treatmentTypeId;

	private String providerId;

	private String status;

	private String notes;

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

	public String getBranchId() {
		return branchId;
	}

	public void setBranchId(String branchId) {
		this.branchId = branchId;
	}

	public String getPatientId() {
		return patientId;
	}

	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}

	public String getAppointmentId() {
		return appointmentId;
	}

	public void setAppointmentId(String appointmentId) {
		this.appointmentId = appointmentId;
	}

	public String getTreatmentTypeId() {
		return treatmentTypeId;
	}

	public void setTreatmentTypeId(String treatmentTypeId) {
		this.treatmentTypeId = treatmentTypeId;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}
