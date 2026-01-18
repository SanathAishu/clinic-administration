package com.clinic.mgmt.treatment.domain;

public enum TreatmentStatus {
	PLANNED("planned"),
	IN_PROGRESS("in_progress"),
	COMPLETED("completed"),
	CANCELLED("cancelled");

	private final String value;

	TreatmentStatus(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static TreatmentStatus fromValue(String value) {
		if (value == null) {
			return null;
		}
		for (TreatmentStatus status : values()) {
			if (status.value.equalsIgnoreCase(value)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unsupported treatment status: " + value);
	}
}
