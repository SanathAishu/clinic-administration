package com.clinic.common.exception;

/**
 * Exception thrown when user lacks permission to access a resource.
 * Results in HTTP 403 Forbidden response.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
