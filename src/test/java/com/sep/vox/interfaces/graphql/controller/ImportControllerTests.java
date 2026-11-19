package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ViewImportRowsQuery;
import com.sep.vox.application.port.input.query.ViewImportSessionQuery;
import com.sep.vox.application.port.input.query.ViewImportSessionsQuery;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportRowsUseCase;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportSessionUseCase;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportSessionsUseCase;
import com.sep.vox.application.response.input.importfile.ImportDataEntryResponse;
import com.sep.vox.application.response.input.importfile.ImportRowErrorResponse;
import com.sep.vox.application.response.input.importfile.ImportRowResponse;
import com.sep.vox.application.response.input.importfile.ImportSessionDetailsResponse;
import com.sep.vox.application.response.input.importfile.ImportSessionSummaryResponse;
import com.sep.vox.domain.common.PageResult;

class ImportControllerTests {

    @Test
    void importSession_should_return_details_from_use_case() {
        ViewImportSessionUseCase detailsUseCase = mock(ViewImportSessionUseCase.class);
        ViewImportSessionsUseCase listUseCase = mock(ViewImportSessionsUseCase.class);
        ViewImportRowsUseCase rowsUseCase = mock(ViewImportRowsUseCase.class);
        ImportController controller = new ImportController(detailsUseCase, listUseCase, rowsUseCase);
        UUID sessionId = UUID.randomUUID();
        ImportSessionDetailsResponse expected = new ImportSessionDetailsResponse(
            sessionId,
            UUID.randomUUID(),
            "SCHOOL_CLASS",
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
            "COMPLETED",
            null,
            "2026-06-09T10:00:00+07:00",
            "2026-06-08T10:00:00+07:00",
            "2026-06-08T10:10:00+07:00"
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
        ImportController controller = new ImportController(detailsUseCase, listUseCase, rowsUseCase);
        ImportSessionSummaryResponse summary = new ImportSessionSummaryResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "SCHOOL_CLASS",
            "classes.csv",
            1L,
            1L,
            0L,
            1L,
            0L,
            "COMPLETED",
            "2026-06-09T10:00:00+07:00",
            "2026-06-08T10:00:00+07:00",
            "2026-06-08T10:10:00+07:00"
        );
        PageResult<ImportSessionSummaryResponse> expected = new PageResult<>(List.of(summary), 1, 20, 1L, 1);
        ViewImportSessionsQuery query = new ViewImportSessionsQuery(1, 20, "SCHOOL_CLASS", "COMPLETED");
        when(listUseCase.execute(query)).thenReturn(expected);

        PageResult<ImportSessionSummaryResponse> result = controller.importSessions(1, 20, "SCHOOL_CLASS", "COMPLETED");

        assertThat(result).isEqualTo(expected);
        assertThat(result.content()).containsExactly(summary);
        verify(listUseCase).execute(query);
    }

    @Test
    void importRows_should_return_page_from_use_case() {
        ViewImportSessionUseCase detailsUseCase = mock(ViewImportSessionUseCase.class);
        ViewImportSessionsUseCase listUseCase = mock(ViewImportSessionsUseCase.class);
        ViewImportRowsUseCase rowsUseCase = mock(ViewImportRowsUseCase.class);
        ImportController controller = new ImportController(detailsUseCase, listUseCase, rowsUseCase);
        UUID sessionId = UUID.randomUUID();
        ImportRowResponse row = new ImportRowResponse(
            UUID.randomUUID(),
            sessionId,
            1L,
            List.of(new ImportDataEntryResponse("Mã lớp", "A01")),
            List.of(new ImportDataEntryResponse("code", "A01")),
            List.of(new ImportRowErrorResponse("code", "Mã lớp đã tồn tại trong hệ thống")),
            "INVALID"
        );
        PageResult<ImportRowResponse> expected = new PageResult<>(List.of(row), 1, 20, 1L, 1);
        ViewImportRowsQuery query = new ViewImportRowsQuery(sessionId, 1, 20, "INVALID");
        when(rowsUseCase.execute(query)).thenReturn(expected);

        PageResult<ImportRowResponse> result = controller.importRows(sessionId, 1, 20, "INVALID");

        assertThat(result).isEqualTo(expected);
        assertThat(result.content()).containsExactly(row);
        verify(rowsUseCase).execute(query);
    }
}
