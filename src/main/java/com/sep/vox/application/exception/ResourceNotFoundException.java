package com.sep.vox.application.exception;

/**
 * Business exception, use when the resource is not found with status of 404
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
