package com.clinic.mgmt.lookup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateLookupValueException extends RuntimeException {

	public DuplicateLookupValueException(String message) {
		super(message);
	}
}
