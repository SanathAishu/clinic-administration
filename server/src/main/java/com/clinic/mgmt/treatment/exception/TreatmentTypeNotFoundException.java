package com.clinic.mgmt.treatment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TreatmentTypeNotFoundException extends RuntimeException {

	public TreatmentTypeNotFoundException(String message) {
		super(message);
	}
}
