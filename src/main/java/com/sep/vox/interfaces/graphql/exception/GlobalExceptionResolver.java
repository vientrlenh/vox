package com.sep.vox.interfaces.graphql.exception;

import com.sep.vox.application.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;

@Component
@Slf4j
public class GlobalExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {

        // The old printStackTrace() only wrote to stderr and bypassed Logback, so it never reached logs/vox.log
        // -- switched to a real logger so INTERNAL_ERROR (the last branch) can be traced after it happens.
        log.error("Unhandled exception resolving field {}", env.getExecutionStepInfo().getPath(), ex);

        ErrorType errorType = errorTypeOf(ex);
        GraphQLError.Builder<?> error = GraphQLError.newError()
            .errorType(errorType)
            .message(errorType == ErrorType.INTERNAL_ERROR ? "An error occurred" : ex.getMessage())
            .path(env.getExecutionStepInfo().getPath())
            .location(env.getField().getSourceLocation());
        if (ex instanceof BusinessRuleViolationException) {
            error.extensions(Map.of("code", "BUSINESS_RULE_VIOLATION"));
        }
        return error.build();
    }

    private ErrorType errorTypeOf(Throwable ex) {
        return switch (ex) {
            case ResourceNotFoundException _ -> ErrorType.NOT_FOUND;
            case ResourceDuplicatedException _, IllegalArgumentException _, BusinessRuleViolationException _ ->
                ErrorType.BAD_REQUEST;
            case UnauthorizedException _, AuthenticationException _ -> ErrorType.UNAUTHORIZED;
            case AccessDeniedException _ -> isUnauthenticated() ? ErrorType.UNAUTHORIZED : ErrorType.FORBIDDEN;
            case ForbiddenException _ -> ErrorType.FORBIDDEN;
            default -> ErrorType.INTERNAL_ERROR;
        };
    }

    private boolean isUnauthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated();
    }
}
