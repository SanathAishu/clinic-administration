package com.clinic.common.exception;

/**
 * Exception thrown when user is not authenticated.
 * Results in HTTP 401 Unauthorized response.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
