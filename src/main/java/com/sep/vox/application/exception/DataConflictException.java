package com.sep.vox.application.exception;

/**
 * Raised by persistence adapters when the database rejects a write because of a constraint (unique, foreign key,
 * not null). It is deliberately not mapped by any exception handler, so a caller that does not catch it ends in the
 * same generic server error as the underlying Spring exception did.
 */
public class DataConflictException extends RuntimeException {
    public DataConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
