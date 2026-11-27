package com.sep.vox.interfaces.grpc.exception;

import org.jspecify.annotations.Nullable;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;

import io.grpc.Status;
import io.grpc.StatusException;

@Component
public class GlobalGrpcExceptionHandler implements GrpcExceptionHandler{

    @Override
    public @Nullable StatusException handleException(Throwable exception) {
        Status status = statusOf(exception);
        if (status == null) {
            return null;
        }
        return status.withDescription(descriptionOf(exception)).asException();
    }

    private @Nullable Status statusOf(Throwable exception) {
        return switch (exception) {
            case IllegalArgumentException _ -> Status.INVALID_ARGUMENT;
            case ResourceNotFoundException _ -> Status.NOT_FOUND;
            case ResourceDuplicatedException _ -> Status.ALREADY_EXISTS;
            case ForbiddenException _, AccessDeniedException _ -> Status.PERMISSION_DENIED;
            case UnauthorizedException _, AuthenticationException _ -> Status.UNAUTHENTICATED;
            case BusinessRuleViolationException _ -> Status.FAILED_PRECONDITION;
            default -> null;
        };
    }

    private @Nullable String descriptionOf(Throwable exception) {
        return switch (exception) {
            case AccessDeniedException _ -> "Access denied";
            case AuthenticationException _ -> "Invalid authentication state";
            default -> exception.getMessage();
        };
    }

}
