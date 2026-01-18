package com.clinic.mgmt.appointment.dto;

import java.time.Instant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AppointmentRequest {

	@NotBlank
	private String organizationId;

	@NotBlank
	private String clinicId;

	@NotBlank
	private String branchId;

	@NotBlank
	private String patientId;

	private String providerId;

	private String departmentId;

	@NotNull
	private Instant scheduledStart;

	@NotNull
	private Instant scheduledEnd;

	private String status;

	private String reason;

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

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public String getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(String departmentId) {
		this.departmentId = departmentId;
	}

	public Instant getScheduledStart() {
		return scheduledStart;
	}

	public void setScheduledStart(Instant scheduledStart) {
		this.scheduledStart = scheduledStart;
	}

	public Instant getScheduledEnd() {
		return scheduledEnd;
	}

	public void setScheduledEnd(Instant scheduledEnd) {
		this.scheduledEnd = scheduledEnd;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}
