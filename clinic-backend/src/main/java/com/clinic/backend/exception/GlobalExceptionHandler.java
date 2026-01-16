package com.clinic.backend.exception;

import com.clinic.common.dto.response.ErrorResponse;
import com.clinic.common.exception.*;
import com.clinic.common.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST controllers.
 *
 * Provides centralized exception handling and standardized error responses
 * across the entire application following RFC 7807 principles.
 *
 * Exception Priority:
 * 1. Security exceptions (401, 403)
 * 2. Business exceptions (400, 404, 409)
 * 3. Validation exceptions (400)
 * 4. Technical exceptions (500)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ========================================
    // CUSTOM APPLICATION EXCEPTIONS
    // ========================================

    /**
     * Handle EntityNotFoundException - 404 Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Entity not found: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .errorCode("ENTITY_NOT_FOUND")
                .path(request.getRequestURI())
                .requestId(generateRequestId(request))
                .tenantId(getTenantId())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle DuplicateEntityException - 409 Conflict
     */
    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEntityException(
            DuplicateEntityException ex,
            HttpServletRequest request) {

        log.warn("Duplicate entity: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .errorCode("DUPLICATE_ENTITY")
                .path(request.getRequestURI())
                .requestId(generateRequestId(request))
                .tenantId(getTenantId())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle BusinessException - 400 Bad Request
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        log.warn("Business rule violation: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode() != null ? ex.getErrorCode() : "BUSINESS_RULE_VIOLATION")
                .path(request.getRequestURI())
                .requestId(generateRequestId(request))
                .tenantId(getTenantId())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle UnauthorizedException - 401 Unauthorized
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex,
            HttpServletRequest request) {

        log.warn("Unauthorized access attempt: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(ex.getMessage())
                .errorCode("UNAUTHORIZED")
                .path(request.getRequestURI())
                .requestId(generateRequestId(request))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle ForbiddenException - 403 Forbidden
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(
            ForbiddenException ex,
            HttpServletRequest request) {

        log.warn("Forbidden access attempt: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(ex.getMessage())
                .errorCode("FORBIDDEN")
                .path(request.getRequestURI())
                .requestId(generateRequestId(request))
                .tenantId(getTenantId())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // ========================================
    // SPRING SECURITY EXCEPTIONS
    // ========================================

    /**
     * Handle Spring Security AuthenticationException - 401 Unauthorized
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {

        log.warn("Authentication failed: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Authentication failed")
                .errorCode("AUTHENTICATION_FAILED")
                .path(request.getRequestURI())
                .requestId(generateRequestId(request))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle BadCredentialsException - 401 Unauthorized
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex,
            HttpServletRequest request) {

        log.warn("Bad credentials: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Invalid username or password")
                .errorCode("BAD_CREDENTIALS")
                .path(request.getRequestURI())
                .requestId(generateRequestId(request))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle Spring Security AccessDeniedException - 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("Access denied: {} - URI: {} - Tenant: {}",
                ex.getMessage(), request.getRequestURI(), getTenantId());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("You don't have permission to access this resource")
                .errorCode("ACCESS_DENIED")
                .path(request.getRequestURI())
                .requestId(generateRequestId(request))
                .tenantId(getTenantId())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // ========================================
    // VALIDATION EXCEPTIONS
    // ========================================

    /**
     * Handle MethodArgumentNotValidException - 400 Bad Request
     * Triggered by @Valid annotation on request bodies
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.warn("Validation failed: {} errors - URI: {}",
                ex.getBindingResult().getErrorCount(), request.getRequestURI());

        List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .collect(Collectors.toList());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .errorCode("VALIDATION_FAILED")
                .path(request.getRequestURI())
                .requestId(generateRequestId(request))
                .tenantId(getTenantId())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle MissingServletRequestParameterException - 400 Bad Request
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        log.warn("Missing required parameter: {} - URI: {}", ex.getParameterName(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(String.format("Required parameter '%s' is missing", ex.getParameterName()))
                .errorCode("MISSING_PARAMETER")
                .path(request.getRequestURI())
                .requestId(generateRequestId(request))
                .tenantId(getTenantId())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle MethodArgumentTypeMismatchException - 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        log.warn("Type mismatch for parameter: {} - URI: {}", ex.getName(), request.getRequestURI());

        String message = String.format("Parameter '%s' has invalid type. Expected %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .errorCode("TYPE_MISMATCH")
                .path(request.getRequestURI())
                .requestId(generateRequestId(request))
                .tenantId(getTenantId())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle HttpMessageNotReadableException - 400 Bad Request
     * Occurs when request body is malformed JSON
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Malformed JSON request: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Malformed JSON request")
                .errorCode("MALFORMED_JSON")
                .path(request.getRequestURI())
                .requestId(generateRequestId(request))
                .tenantId(getTenantId())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ========================================
    // HTTP EXCEPTIONS
    // ========================================

    /**
     * Handle NoHandlerFoundException - 404 Not Found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        log.warn("No handler found: {} {} - URI: {}",
                ex.getHttpMethod(), ex.getRequestURL(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(String.format("No endpoint %s %s", ex.getHttpMethod(), ex.getRequestURL()))
                .errorCode("ENDPOINT_NOT_FOUND")
                .path(request.getRequestURI())
                .requestId(generateRequestId(request))
                .tenantId(getTenantId())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle HttpRequestMethodNotSupportedException - 405 Method Not Allowed
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        log.warn("Method not supported: {} - URI: {}", ex.getMethod(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase())
                .message(String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()))
                .errorCode("METHOD_NOT_ALLOWED")
                .path(request.getRequestURI())
                .requestId(generateRequestId(request))
                .tenantId(getTenantId())
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    // ========================================
    // DATABASE EXCEPTIONS
    // ========================================

    /**
     * Handle DataIntegrityViolationException - 409 Conflict
     * Triggered by unique constraint violations, foreign key violations, etc.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.error("Data integrity violation: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        String message = "Data integrity violation. The operation violates database constraints.";

        // Extract more specific message if available
        if (ex.getRootCause() != null) {
            String rootMessage = ex.getRootCause().getMessage();
            if (rootMessage != null) {
                if (rootMessage.contains("unique constraint") || rootMessage.contains("duplicate key")) {
                    message = "A record with this value already exists";
                } else if (rootMessage.contains("foreign key")) {
                    message = "Cannot perform operation due to related records";
                } else if (rootMessage.contains("not-null")) {
                    message = "Required field is missing";
                }
            }
        }

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(message)
                .errorCode("DATA_INTEGRITY_VIOLATION")
                .path(request.getRequestURI())
                .requestId(generateRequestId(request))
                .tenantId(getTenantId())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ========================================
    // GENERIC EXCEPTION HANDLER
    // ========================================

    /**
     * Handle all other uncaught exceptions - 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error: {} - URI: {}", ex.getMessage(), request.getRequestURI(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred. Please contact support if the problem persists.")
                .errorCode("INTERNAL_SERVER_ERROR")
                .path(request.getRequestURI())
                .requestId(generateRequestId(request))
                .tenantId(getTenantId())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Map FieldError to ValidationError DTO
     */
    private ErrorResponse.ValidationError mapFieldError(FieldError fieldError) {
        return ErrorResponse.ValidationError.builder()
                .field(fieldError.getField())
                .rejectedValue(fieldError.getRejectedValue())
                .message(fieldError.getDefaultMessage())
                .build();
    }

    /**
     * Generate or retrieve request ID for tracing
     */
    private String generateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }

    /**
     * Get current tenant ID from TenantContext
     */
    private String getTenantId() {
        try {
            UUID tenantId = TenantContext.getCurrentTenant();
            return tenantId != null ? tenantId.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
