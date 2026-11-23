package com.sep.vox.interfaces.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.exception.ResourceNotFoundException;

import graphql.GraphQLError;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;

class GlobalExceptionResolverTests {

    private final GlobalExceptionResolver resolver = new GlobalExceptionResolver();
    private DataFetchingEnvironment environment;

    @BeforeEach
    void setUp() {
        environment = mock(DataFetchingEnvironment.class);
        ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
        when(stepInfo.getPath()).thenReturn(ResultPath.rootPath());
        when(environment.getExecutionStepInfo()).thenReturn(stepInfo);
        when(environment.getField()).thenReturn(Field.newField("field").build());
    }

    private GraphQLError resolve(Throwable exception) {
        return resolver.resolveToSingleError(exception, environment);
    }

    @Test
    void businessRuleViolationAnswersBadRequestWithBusinessRuleViolationCode() {
        GraphQLError error = resolve(new BusinessRuleViolationException("no quota"));

        assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(error.getMessage()).isEqualTo("no quota");
        assertThat(error.getExtensions()).containsEntry("code", "BUSINESS_RULE_VIOLATION");
    }

    @Test
    void resourceNotFoundAnswersNotFound() {
        assertThat(resolve(new ResourceNotFoundException("missing")).getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
    }

    @Test
    void illegalArgumentAnswersBadRequest() {
        GraphQLError error = resolve(new IllegalArgumentException("bad"));

        assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(error.getMessage()).isEqualTo("bad");
    }

    @Test
    void illegalStateAnswersInternalErrorWithoutItsMessage() {
        GraphQLError error = resolve(new IllegalStateException("secret internal detail"));

        assertThat(error.getErrorType()).isEqualTo(ErrorType.INTERNAL_ERROR);
        assertThat(error.getMessage()).doesNotContain("secret internal detail");
    }
}
