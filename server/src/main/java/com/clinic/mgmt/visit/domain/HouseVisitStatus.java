package com.clinic.mgmt.visit.domain;

public enum HouseVisitStatus {
	SCHEDULED("scheduled"),
	EN_ROUTE("en_route"),
	COMPLETED("completed"),
	CANCELLED("cancelled");

	private final String value;

	HouseVisitStatus(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static HouseVisitStatus fromValue(String value) {
		if (value == null) {
			return null;
		}
		for (HouseVisitStatus status : values()) {
			if (status.value.equalsIgnoreCase(value)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unsupported house visit status: " + value);
	}
}
