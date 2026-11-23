package com.sep.vox.application.exception;

public class ResourceDuplicatedException extends RuntimeException {
    public ResourceDuplicatedException(String message) {
        super(message);
    }
}
