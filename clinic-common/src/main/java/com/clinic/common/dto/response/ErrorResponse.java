package com.clinic.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standardized error response for all API errors.
 *
 * Follows RFC 7807 (Problem Details for HTTP APIs) principles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Timestamp when the error occurred
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * HTTP status code
     */
    private int status;

    /**
     * HTTP status reason phrase
     */
    private String error;

    /**
     * High-level error message
     */
    private String message;

    /**
     * Application-specific error code (optional)
     */
    private String errorCode;

    /**
     * Request path that caused the error
     */
    private String path;

    /**
     * Unique request ID for tracing
     */
    private String requestId;

    /**
     * Tenant ID (for multi-tenant context)
     */
    private String tenantId;

    /**
     * Validation errors (for validation failures)
     */
    private List<ValidationError> validationErrors;

    /**
     * Additional error details
     */
    private Map<String, Object> details;

    /**
     * Represents a single validation error
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {

        /**
         * Field that failed validation
         */
        private String field;

        /**
         * Rejected value
         */
        private Object rejectedValue;

        /**
         * Validation error message
         */
        private String message;
    }
}
