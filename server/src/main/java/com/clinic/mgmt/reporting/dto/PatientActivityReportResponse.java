package com.clinic.mgmt.reporting.dto;

public class PatientActivityReportResponse {

	private String day;

	private Long newPatients;

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

	public Long getNewPatients() {
		return newPatients;
	}

	public void setNewPatients(Long newPatients) {
		this.newPatients = newPatients;
	}
}
