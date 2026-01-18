package com.clinic.mgmt.treatment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateTreatmentTypeException extends RuntimeException {

	public DuplicateTreatmentTypeException(String message) {
		super(message);
	}
}
