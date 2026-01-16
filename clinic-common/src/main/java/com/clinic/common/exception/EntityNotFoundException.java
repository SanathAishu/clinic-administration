package com.clinic.common.exception;

/**
 * Exception thrown when a requested entity is not found.
 * Results in HTTP 404 Not Found response.
 */
public class EntityNotFoundException extends RuntimeException {

    private final String entityName;
    private final Object identifier;

    public EntityNotFoundException(String entityName, Object identifier) {
        super(String.format("%s not found with identifier: %s", entityName, identifier));
        this.entityName = entityName;
        this.identifier = identifier;
    }

    public EntityNotFoundException(String message) {
        super(message);
        this.entityName = null;
        this.identifier = null;
    }

    public String getEntityName() {
        return entityName;
    }

    public Object getIdentifier() {
        return identifier;
    }
}
