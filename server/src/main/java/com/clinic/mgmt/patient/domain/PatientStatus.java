package com.clinic.mgmt.patient.domain;

public enum PatientStatus {
	ACTIVE("active"),
	INACTIVE("inactive"),
	ARCHIVED("archived");

	private final String value;

	PatientStatus(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static PatientStatus fromValue(String value) {
		if (value == null) {
			return null;
		}
		for (PatientStatus status : values()) {
			if (status.value.equalsIgnoreCase(value)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unsupported patient status: " + value);
	}
}
