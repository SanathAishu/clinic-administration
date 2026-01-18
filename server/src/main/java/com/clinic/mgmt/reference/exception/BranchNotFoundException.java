package com.clinic.mgmt.reference.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BranchNotFoundException extends RuntimeException {

	public BranchNotFoundException(String message) {
		super(message);
	}
}
