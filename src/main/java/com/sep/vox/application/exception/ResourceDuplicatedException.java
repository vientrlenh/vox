package com.sep.vox.application.exception;

/**
    Business exception, response with status code of 409
 */
public class ResourceDuplicatedException extends RuntimeException {
    public ResourceDuplicatedException(String message) {
        super(message);
    }
}
