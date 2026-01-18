package com.clinic.mgmt.appointment.domain;

public enum AppointmentStatus {
	SCHEDULED("scheduled"),
	IN_PROGRESS("in_progress"),
	COMPLETED("completed"),
	CANCELLED("cancelled"),
	NO_SHOW("no_show");

	private final String value;

	AppointmentStatus(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static AppointmentStatus fromValue(String value) {
		if (value == null) {
			return null;
		}
		for (AppointmentStatus status : values()) {
			if (status.value.equalsIgnoreCase(value)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unsupported appointment status: " + value);
	}
}
