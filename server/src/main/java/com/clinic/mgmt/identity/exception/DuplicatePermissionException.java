package com.clinic.mgmt.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicatePermissionException extends RuntimeException {

	public DuplicatePermissionException(String message) {
		super(message);
	}
}
