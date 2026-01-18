package com.clinic.mgmt.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RoleInUseException extends RuntimeException {

	public RoleInUseException(String message) {
		super(message);
	}
}
