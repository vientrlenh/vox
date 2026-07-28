package com.sep.vox.application.usecase.importfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.ImportCommitHandler;
import com.sep.vox.application.port.input.service.ImportCommitResult;
import com.sep.vox.application.port.input.service.ImportCommitService;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;

class ImportCommitServiceTests {

    private ImportSessionRepository importSessionRepository;
    private ImportRowRepository importRowRepository;
    private ImportCommitHandler handler;

    @BeforeEach
    void setUp() {
        importSessionRepository = mock(ImportSessionRepository.class);
        importRowRepository = mock(ImportRowRepository.class);
        handler = mock(ImportCommitHandler.class);
        when(handler.supportedType()).thenReturn(ImportType.USER);
    }

    @Test
    void should_complete_session_and_save_rows_on_success() {
        var sessionId = UUID.randomUUID();
        var session = session(sessionId);
        var rows = List.of(row(sessionId, 1L), row(sessionId, 2L));

        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(importRowRepository.findBySessionId(sessionId)).thenReturn(rows);
        when(handler.commit(session, rows)).thenReturn(new ImportCommitResult(2L, 0L, 0L, 0L));

        service().commit(sessionId);

        assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.COMPLETED);
        assertThat(session.getImportedRows()).isEqualTo(2L);
        verify(importRowRepository).saveAll(rows);
        verify(importSessionRepository).save(session);
    }

    @Test
    void should_save_row_progress_when_handler_fails_midway() {
        var sessionId = UUID.randomUUID();
        var session = session(sessionId);
        var importedRow = row(sessionId, 1L);
        var pendingRow = row(sessionId, 2L);
        var rows = List.of(importedRow, pendingRow);

        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(importRowRepository.findBySessionId(sessionId)).thenReturn(rows);
        when(handler.commit(session, rows)).thenAnswer(invocation -> {
            importedRow.setStatus(ImportRowStatus.IMPORTED);
            throw new RejectedExecutionException("mail queue full");
        });

        assertThatCode(() -> service().commit(sessionId)).doesNotThrowAnyException();

        // Dòng đã xử lý xong trước khi lỗi phải được lưu lại, không bị mất trạng thái.
        assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.FAILED);
        assertThat(session.getFailureReason()).contains("mail queue full");
        assertThat(importedRow.getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        verify(importRowRepository).saveAll(rows);
        verify(importSessionRepository).save(session);
    }

    @Test
    void should_not_touch_rows_when_session_type_has_no_handler() {
        var sessionId = UUID.randomUUID();
        var session = session(sessionId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        var serviceWithoutHandler = new ImportCommitService(List.of(), importSessionRepository, importRowRepository);

        assertThatCode(() -> serviceWithoutHandler.commit(sessionId)).isInstanceOf(IllegalStateException.class);
        verify(importRowRepository, never()).saveAll(any());
    }

    private ImportCommitService service() {
        return new ImportCommitService(List.of(handler), importSessionRepository, importRowRepository);
    }

    private ImportSession session(UUID id) {
        return new ImportSession(
            id, UUID.randomUUID(), ImportType.USER, "users.csv", "[]", "{}", "{}",
            0L, 0L, 0L, 0L, 0L, null, ImportSessionStatus.IMPORTING, null,
            OffsetDateTime.now().plusDays(1), null, null, null, 0,
            OffsetDateTime.now(), OffsetDateTime.now(),
            UUID.randomUUID(), UUID.randomUUID()
        );
    }

    private ImportRow row(UUID sessionId, long rowNumber) {
        return new ImportRow(UUID.randomUUID(), sessionId, rowNumber, "{}", null, null, ImportRowStatus.PENDING);
    }
}
