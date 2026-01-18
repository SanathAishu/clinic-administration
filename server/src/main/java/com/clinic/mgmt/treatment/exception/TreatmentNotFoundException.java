package com.clinic.mgmt.treatment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TreatmentNotFoundException extends RuntimeException {

	public TreatmentNotFoundException(String message) {
		super(message);
	}
}
