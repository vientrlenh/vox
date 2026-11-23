package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.importfile.ViewImportRowsQuery;
import com.sep.vox.application.port.input.query.importfile.ViewImportSessionQuery;
import com.sep.vox.application.port.input.query.importfile.ViewImportSessionsQuery;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportRowsUseCase;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportSessionUseCase;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportSessionsUseCase;
import com.sep.vox.application.response.input.importfile.ImportDataEntryResponse;
import com.sep.vox.application.response.input.importfile.ImportRowErrorResponse;
import com.sep.vox.application.response.input.importfile.ImportRowResponse;
import com.sep.vox.application.response.input.importfile.ImportSessionDetailsResponse;
import com.sep.vox.application.response.input.importfile.ImportSessionSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;

class GraphQLImportControllerTests {

    @Test
    void importSession_should_return_details_from_use_case() {
        ViewImportSessionUseCase detailsUseCase = mock(ViewImportSessionUseCase.class);
        ViewImportSessionsUseCase listUseCase = mock(ViewImportSessionsUseCase.class);
        ViewImportRowsUseCase rowsUseCase = mock(ViewImportRowsUseCase.class);
        GraphQLImportController controller = new GraphQLImportController(detailsUseCase, listUseCase, rowsUseCase);
        UUID sessionId = UUID.randomUUID();
        ImportSessionDetailsResponse expected = new ImportSessionDetailsResponse(
            sessionId,
            UUID.randomUUID(),
            ImportType.SCHOOL_CLASS,
            "classes.csv",
            List.of("code"),
            List.of(),
            List.of(),
            1L,
            0L,
            1L,
            0L,
            1L,
            null,
            ImportSessionStatus.COMPLETED,
            null,
            Instant.parse("2026-06-09T03:00:00Z"),
            Instant.parse("2026-06-08T03:00:00Z"),
            Instant.parse("2026-06-08T03:10:00Z")
        );
        when(detailsUseCase.execute(new ViewImportSessionQuery(sessionId))).thenReturn(expected);

        ImportSessionDetailsResponse result = controller.importSession(sessionId);

        assertThat(result).isEqualTo(expected);
        verify(detailsUseCase).execute(new ViewImportSessionQuery(sessionId));
    }

    @Test
    void importSessions_should_return_page_from_use_case() {
        ViewImportSessionUseCase detailsUseCase = mock(ViewImportSessionUseCase.class);
        ViewImportSessionsUseCase listUseCase = mock(ViewImportSessionsUseCase.class);
        ViewImportRowsUseCase rowsUseCase = mock(ViewImportRowsUseCase.class);
        GraphQLImportController controller = new GraphQLImportController(detailsUseCase, listUseCase, rowsUseCase);
        ImportSessionSummaryResponse summary = new ImportSessionSummaryResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            ImportType.SCHOOL_CLASS,
            "classes.csv",
            1L,
            1L,
            0L,
            1L,
            0L,
            ImportSessionStatus.COMPLETED,
            Instant.parse("2026-06-09T03:00:00Z"),
            Instant.parse("2026-06-08T03:00:00Z"),
            Instant.parse("2026-06-08T03:10:00Z")
        );
        PageResult<ImportSessionSummaryResponse> expected = new PageResult<>(List.of(summary), 1, 20, 1L, 1);
        ViewImportSessionsQuery query = new ViewImportSessionsQuery(1, 20, ImportType.SCHOOL_CLASS, ImportSessionStatus.COMPLETED);
        when(listUseCase.execute(query)).thenReturn(expected);

        PageResult<ImportSessionSummaryResponse> result = controller.importSessions(1, 20, ImportType.SCHOOL_CLASS, ImportSessionStatus.COMPLETED);

        assertThat(result).isEqualTo(expected);
        assertThat(result.content()).containsExactly(summary);
        verify(listUseCase).execute(query);
    }

    @Test
    void importRows_should_return_page_from_use_case() {
        ViewImportSessionUseCase detailsUseCase = mock(ViewImportSessionUseCase.class);
        ViewImportSessionsUseCase listUseCase = mock(ViewImportSessionsUseCase.class);
        ViewImportRowsUseCase rowsUseCase = mock(ViewImportRowsUseCase.class);
        GraphQLImportController controller = new GraphQLImportController(detailsUseCase, listUseCase, rowsUseCase);
        UUID sessionId = UUID.randomUUID();
        ImportRowResponse row = new ImportRowResponse(
            UUID.randomUUID(),
            sessionId,
            1L,
            List.of(new ImportDataEntryResponse("Mã lớp", "A01")),
            List.of(new ImportDataEntryResponse("code", "A01")),
            List.of(new ImportRowErrorResponse("code", "Mã lớp đã tồn tại trong hệ thống")),
            ImportRowStatus.INVALID
        );
        PageResult<ImportRowResponse> expected = new PageResult<>(List.of(row), 1, 20, 1L, 1);
        ViewImportRowsQuery query = new ViewImportRowsQuery(sessionId, 1, 20, ImportRowStatus.INVALID);
        when(rowsUseCase.execute(query)).thenReturn(expected);

        PageResult<ImportRowResponse> result = controller.importRows(sessionId, 1, 20, ImportRowStatus.INVALID);

        assertThat(result).isEqualTo(expected);
        assertThat(result.content()).containsExactly(row);
        verify(rowsUseCase).execute(query);
    }
}
