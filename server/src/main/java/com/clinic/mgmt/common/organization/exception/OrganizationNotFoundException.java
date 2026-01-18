package com.clinic.mgmt.common.organization.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrganizationNotFoundException extends RuntimeException {

	public OrganizationNotFoundException(String message) {
		super(message);
	}
}
