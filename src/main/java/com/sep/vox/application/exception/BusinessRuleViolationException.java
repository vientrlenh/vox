package com.sep.vox.application.exception;

/**
 * Thrown when a valid request is refused because a business rule forbids it, for example an exhausted quota,
 * a plan limit or a missing wallet draw confirmation. It is not a bad request (use
 * {@link IllegalArgumentException}) and not an internal fault (use {@link IllegalStateException}).
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
