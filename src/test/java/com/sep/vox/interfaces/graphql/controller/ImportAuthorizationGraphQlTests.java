package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.usecase.importfile.ViewImportRowsUseCase;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportSessionUseCase;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportSessionsUseCase;
import com.sep.vox.application.response.input.importfile.ImportSessionDetailsResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Runs the three {@link ImportController} queries through the real schema and {@code @PreAuthorize}, to prove
 * that {@code importSessions} is school admin only while {@code importSession} and {@code importRows} also admit
 * teachers and system admins. The direct-call behaviour stays covered by {@link ImportControllerTests}.
 */
@GraphQlControllerTest(ImportController.class)
class ImportAuthorizationGraphQlTests {

    private static final UUID ID = UUID.randomUUID();

    private static final String IMPORT_SESSION_QUERY = "{ importSession(id: \"%s\") { id } }".formatted(ID);
    private static final String IMPORT_SESSIONS_QUERY = "{ importSessions { totalElements } }";
    private static final String IMPORT_ROWS_QUERY =
        "{ importRows(sessionId: \"%s\") { totalElements } }".formatted(ID);

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewImportSessionUseCase viewImportSessionUseCase;
    @MockitoBean
    private ViewImportSessionsUseCase viewImportSessionsUseCase;
    @MockitoBean
    private ViewImportRowsUseCase viewImportRowsUseCase;

    private static <T> PageResult<T> emptyPage() {
        return new PageResult<>(List.of(), 1, 20, 0L, 0);
    }

    private static ImportSessionDetailsResponse sessionWithOnlyId() {
        return new ImportSessionDetailsResponse(ID, null, null, null, null, null, null, 0, 0, 0, 0, 0, null, null,
            null, null, null, null);
    }

    private void assertErrorType(String document, ErrorType expectedErrorType) {
        graphQlTester.document(document).execute()
            .errors().expect(error -> error.getErrorType() == expectedErrorType).verify();
    }

    private void assertTotalElementsIsZero(String document, String operationName) {
        graphQlTester.document(document).execute()
            .errors().verify()
            .path(operationName + ".totalElements").entity(Integer.class).isEqualTo(0);
    }

    private void verifyNoUseCaseWasCalled() {
        verifyNoInteractions(viewImportSessionUseCase, viewImportSessionsUseCase, viewImportRowsUseCase);
    }

    private static Stream<String> everyQuery() {
        return Stream.of(IMPORT_SESSION_QUERY, IMPORT_SESSIONS_QUERY, IMPORT_ROWS_QUERY);
    }

    private void assertSessionAndRowsReturnData() {
        when(viewImportSessionUseCase.execute(any())).thenReturn(sessionWithOnlyId());
        when(viewImportRowsUseCase.execute(any())).thenReturn(emptyPage());

        graphQlTester.document(IMPORT_SESSION_QUERY).execute()
            .errors().verify()
            .path("importSession.id").entity(String.class).isEqualTo(ID.toString());
        assertTotalElementsIsZero(IMPORT_ROWS_QUERY, "importRows");
    }

    @ParameterizedTest
    @MethodSource("everyQuery")
    @WithAnonymousUser
    void every_query_should_be_unauthorized_without_authentication(String document) {
        assertErrorType(document, ErrorType.UNAUTHORIZED);

        verifyNoUseCaseWasCalled();
    }

    @ParameterizedTest
    @MethodSource("everyQuery")
    @WithMockUser(roles = "STUDENT")
    void every_query_should_be_forbidden_for_student(String document) {
        assertErrorType(document, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void import_sessions_should_be_forbidden_for_teacher() {
        assertErrorType(IMPORT_SESSIONS_QUERY, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void import_sessions_should_be_forbidden_for_system_admin() {
        assertErrorType(IMPORT_SESSIONS_QUERY, ErrorType.FORBIDDEN);

        verifyNoUseCaseWasCalled();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void every_query_should_return_data_for_school_admin() {
        when(viewImportSessionsUseCase.execute(any())).thenReturn(emptyPage());

        assertSessionAndRowsReturnData();
        assertTotalElementsIsZero(IMPORT_SESSIONS_QUERY, "importSessions");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void session_and_rows_queries_should_return_data_for_teacher() {
        assertSessionAndRowsReturnData();
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void session_and_rows_queries_should_return_data_for_system_admin() {
        assertSessionAndRowsReturnData();
    }
}
