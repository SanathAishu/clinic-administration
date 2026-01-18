package com.clinic.mgmt.reference.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateClinicException extends RuntimeException {

	public DuplicateClinicException(String message) {
		super(message);
	}
}
