package com.sep.vox.interfaces.graphql.exception;

import com.sep.vox.application.exception.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.exception.QuotaExceededException;
import com.sep.vox.application.exception.UnauthorizedException;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;

@Component
public class GlobalExceptionResolver extends DataFetcherExceptionResolverAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionResolver.class);

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {

        // printStackTrace() cũ chỉ in ra stderr, không qua Logback nên không vào được logs/vox.log --
        // đổi sang logger thật để có thể tra lỗi INTERNAL_ERROR (nhánh dưới cùng) sau khi xảy ra.
        LOGGER.error("Unhandled exception resolving field {}", env.getExecutionStepInfo().getPath(), ex);

        if (ex instanceof NotFoundException) {
            return GraphQLError.newError()
                .errorType(ErrorType.NOT_FOUND)
                .message(ex.getMessage())
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
        }

        if (ex instanceof DuplicatedException) {
            return GraphQLError.newError()
                .errorType(ErrorType.BAD_REQUEST)
                .message(ex.getMessage())
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
        }

        if (ex instanceof IllegalArgumentException) {
            return GraphQLError.newError()
                .errorType(ErrorType.BAD_REQUEST)
                .message(ex.getMessage())
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
        }

        if (ex instanceof IllegalStateException) {
            return GraphQLError.newError()
                .errorType(ErrorType.BAD_REQUEST)
                .message(ex.getMessage())
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
        }

        if (ex instanceof UnauthorizedException) {
            return GraphQLError.newError()
                .errorType(ErrorType.UNAUTHORIZED)
                .message(ex.getMessage())
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
        }

        if (ex instanceof AuthenticationException) {
            return GraphQLError.newError()
                .errorType(ErrorType.UNAUTHORIZED)
                .message(ex.getMessage())
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
        }

        if (ex instanceof AccessDeniedException) {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            var unauthenticated = authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated();
            return GraphQLError.newError()
                    .errorType(unauthenticated ? ErrorType.UNAUTHORIZED : ErrorType.FORBIDDEN)
                    .message(ex.getMessage())
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }

        // ĐÃ THÊM: Xử lý ForbiddenException bạn ném ra từ UseCase
        if (ex instanceof ForbiddenException) {
            return GraphQLError.newError()
                    .errorType(ErrorType.FORBIDDEN)
                    .message(ex.getMessage())
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }

        if (ex instanceof PlanLimitExceededException) {
            return GraphQLError.newError()
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(ex.getMessage())
                    .extensions(Map.of("code", "PLAN_LIMIT_EXCEEDED"))
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }

        if (ex instanceof QuotaExceededException) {
            return GraphQLError.newError()
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(ex.getMessage())
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }



        return GraphQLError.newError()
            .errorType(ErrorType.INTERNAL_ERROR)
            .message("Có lỗi xảy ra")
            .path(env.getExecutionStepInfo().getPath())
            .location(env.getField().getSourceLocation())
            .build();
    }
}
