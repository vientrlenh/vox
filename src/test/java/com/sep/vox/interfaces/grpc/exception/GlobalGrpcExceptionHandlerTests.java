package com.sep.vox.interfaces.grpc.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;

import io.grpc.Status;
import io.grpc.StatusException;

class GlobalGrpcExceptionHandlerTests {

    private final GlobalGrpcExceptionHandler handler = new GlobalGrpcExceptionHandler();

    @Test
    void businessRuleViolationAnswersFailedPrecondition() {
        StatusException result = handler.handleException(new BusinessRuleViolationException("no quota"));

        assertThat(result.getStatus().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
        assertThat(result.getStatus().getDescription()).isEqualTo("no quota");
    }

    @Test
    void illegalArgumentAnswersInvalidArgument() {
        StatusException result = handler.handleException(new IllegalArgumentException("bad"));

        assertThat(result.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void resourceNotFoundAnswersNotFound() {
        StatusException result = handler.handleException(new ResourceNotFoundException("missing"));

        assertThat(result.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    void resourceDuplicatedAnswersAlreadyExists() {
        StatusException result = handler.handleException(new ResourceDuplicatedException("exists"));

        assertThat(result.getStatus().getCode()).isEqualTo(Status.Code.ALREADY_EXISTS);
    }

    @Test
    void unknownExceptionIsLeftToOtherHandlers() {
        assertThat(handler.handleException(new RuntimeException("x"))).isNull();
    }
}
