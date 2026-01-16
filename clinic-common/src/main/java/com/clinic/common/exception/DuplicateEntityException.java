package com.clinic.common.exception;

/**
 * Exception thrown when attempting to create an entity that already exists.
 * Results in HTTP 409 Conflict response.
 */
public class DuplicateEntityException extends RuntimeException {

    private final String entityName;
    private final String fieldName;
    private final Object fieldValue;

    public DuplicateEntityException(String entityName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: %s", entityName, fieldName, fieldValue));
        this.entityName = entityName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public DuplicateEntityException(String message) {
        super(message);
        this.entityName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}
