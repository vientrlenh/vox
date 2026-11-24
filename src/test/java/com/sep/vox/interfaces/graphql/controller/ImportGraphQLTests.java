package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.query.importfile.ViewImportRowsQuery;
import com.sep.vox.application.port.input.query.importfile.ViewImportSessionsQuery;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportRowsUseCase;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportSessionUseCase;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportSessionsUseCase;
import com.sep.vox.application.response.input.importfile.ImportRowResponse;
import com.sep.vox.application.response.input.importfile.ImportSessionDetailsResponse;
import com.sep.vox.application.response.input.importfile.ImportSessionSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.support.GraphQLControllerTest;

/**
 * Sends import queries through the real schema and asserts the typed values: the {@code ImportType},
 * {@code ImportSessionStatus} and {@code ImportRowStatus} enums and the {@code DateTime} scalar, on output and on
 * input.
 */
@GraphQLControllerTest(GraphQLImportController.class)
class ImportGraphQLTests {

    private static final UUID SESSION_ID = UUID.randomUUID();

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewImportSessionUseCase viewImportSessionUseCase;
    @MockitoBean
    private ViewImportSessionsUseCase viewImportSessionsUseCase;
    @MockitoBean
    private ViewImportRowsUseCase viewImportRowsUseCase;

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void import_session_should_return_enums_and_date_time_values() {
        when(viewImportSessionUseCase.execute(any())).thenReturn(sessionDetails());

        graphQlTester.document("{ importSession(id: \"%s\") { type status expiresAt createdAt updatedAt } }"
                .formatted(SESSION_ID))
            .execute()
            .errors().verify()
            .path("importSession.type").entity(String.class).isEqualTo("SCHOOL_CLASS")
            .path("importSession.status").entity(String.class).isEqualTo("COMPLETED")
            .path("importSession.expiresAt").entity(String.class).isEqualTo("2026-06-09T03:00:00Z")
            .path("importSession.createdAt").entity(String.class).isEqualTo("2026-06-08T03:00:00Z")
            .path("importSession.updatedAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void import_sessions_should_return_summary_enums_and_bind_enum_variables() {
        when(viewImportSessionsUseCase.execute(any())).thenReturn(
            new PageResult<>(List.of(sessionSummary()), 1, 20, 1L, 1));

        graphQlTester.document("""
                query($type: ImportType, $status: ImportSessionStatus) {
                    importSessions(type: $type, status: $status) { content { type status createdAt } }
                }""")
            .variable("type", "SCHOOL_CLASS")
            .variable("status", "COMPLETED")
            .execute()
            .errors().verify()
            .path("importSessions.content[0].type").entity(String.class).isEqualTo("SCHOOL_CLASS")
            .path("importSessions.content[0].status").entity(String.class).isEqualTo("COMPLETED")
            .path("importSessions.content[0].createdAt").entity(String.class).isEqualTo("2026-06-08T03:00:00Z");

        verify(viewImportSessionsUseCase).execute(
            new ViewImportSessionsQuery(1, 20, ImportType.SCHOOL_CLASS, ImportSessionStatus.COMPLETED));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void import_sessions_should_reject_an_unknown_status_as_a_validation_error() {
        graphQlTester.document("{ importSessions(status: NOT_A_STATUS) { totalElements } }")
            .execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("NOT_A_STATUS"))
            .verify();

        verifyNoInteractions(viewImportSessionsUseCase);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void import_rows_should_return_status_enum_and_bind_the_status_variable() {
        when(viewImportRowsUseCase.execute(any())).thenReturn(new PageResult<>(
            List.of(new ImportRowResponse(UUID.randomUUID(), SESSION_ID, 3L, List.of(), List.of(), List.of(),
                ImportRowStatus.INVALID)), 1, 20, 1L, 1));

        graphQlTester.document("""
                query($status: ImportRowStatus) {
                    importRows(sessionId: "%s", status: $status) { content { status } }
                }""".formatted(SESSION_ID))
            .variable("status", "INVALID")
            .execute()
            .errors().verify()
            .path("importRows.content[0].status").entity(String.class).isEqualTo("INVALID");

        verify(viewImportRowsUseCase).execute(new ViewImportRowsQuery(SESSION_ID, 1, 20, ImportRowStatus.INVALID));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void import_rows_should_reject_a_session_status_value_as_a_validation_error() {
        graphQlTester.document("{ importRows(sessionId: \"%s\", status: COMPLETED) { totalElements } }"
                .formatted(SESSION_ID))
            .execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("COMPLETED"))
            .verify();

        verifyNoInteractions(viewImportRowsUseCase);
    }

    private static ImportSessionDetailsResponse sessionDetails() {
        return new ImportSessionDetailsResponse(SESSION_ID, null, ImportType.SCHOOL_CLASS, "classes.csv", List.of(),
            List.of(), List.of(), 1L, 0L, 1L, 0L, 1L, null, ImportSessionStatus.COMPLETED, null,
            Instant.parse("2026-06-09T03:00:00Z"), Instant.parse("2026-06-08T03:00:00Z"), null);
    }

    private static ImportSessionSummaryResponse sessionSummary() {
        return new ImportSessionSummaryResponse(SESSION_ID, null, ImportType.SCHOOL_CLASS, "classes.csv", 1L, 1L, 0L,
            1L, 0L, ImportSessionStatus.COMPLETED, null, Instant.parse("2026-06-08T03:00:00Z"), null);
    }
}
