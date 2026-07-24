package com.sep.vox.interfaces.grpc.exception;

import org.jspecify.annotations.Nullable;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.exception.QuotaExceededException;
import com.sep.vox.application.exception.UnauthorizedException;

import io.grpc.Status;
import io.grpc.StatusException;

@Component
public class GlobalGrpcExceptionHandler implements GrpcExceptionHandler{

    @Override
    public @Nullable StatusException handleException(Throwable exception) {
        if (exception instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT
                .withDescription(exception.getMessage())
                .asException();
        }
        if (exception instanceof NotFoundException) {
            return Status.NOT_FOUND
                .withDescription(exception.getMessage())
                .asException();
        }
        if (exception instanceof DuplicatedException) {
            return Status.ALREADY_EXISTS
                .withDescription(exception.getMessage())
                .asException();
        }
        if (exception instanceof ForbiddenException) {
            return Status.PERMISSION_DENIED
                .withDescription(exception.getMessage())
                .asException();
        }
        if (exception instanceof UnauthorizedException) {
            return Status.UNAUTHENTICATED
                .withDescription(exception.getMessage())
                .asException();
        }
        if (exception instanceof QuotaExceededException) {
            return Status.RESOURCE_EXHAUSTED
                .withDescription(exception.getMessage())
                .asException();
        }
        if (exception instanceof PlanLimitExceededException) {
            return Status.FAILED_PRECONDITION
                .withDescription(exception.getMessage())
                .asException();
        }
        if (exception instanceof AccessDeniedException) {
            return Status.PERMISSION_DENIED
                .withDescription("Quyền truy cập bị từ chối")
                .asException();
        }
        if (exception instanceof AuthenticationException) {
            return Status.UNAUTHENTICATED
                .withDescription("Trạng thái đăng nhập không hợp lệ")
                .asException();
        }
        return null;
    }
    
}
