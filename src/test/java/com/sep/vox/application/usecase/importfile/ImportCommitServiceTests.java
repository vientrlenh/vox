package com.sep.vox.application.usecase.importfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.ImportCommitHandler;
import com.sep.vox.application.port.input.service.ImportCommitResult;
import com.sep.vox.application.port.input.service.ImportCommitService;
import com.sep.vox.application.port.output.AuditActorScopePort;
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
    private AuditActorScopePort auditActorScopePort;

    @BeforeEach
    void setUp() {
        importSessionRepository = mock(ImportSessionRepository.class);
        importRowRepository = mock(ImportRowRepository.class);
        handler = mock(ImportCommitHandler.class);
        auditActorScopePort = mock(AuditActorScopePort.class);
        when(auditActorScopePort.callAs(any(), any())).thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(1).get());
        when(handler.supportedType()).thenReturn(ImportType.USER);
    }

    @Test
    void should_complete_session_and_save_rows_on_success() {
        UUID sessionId = UUID.randomUUID();
        ImportSession session = session(sessionId);
        List<ImportRow> rows = List.of(row(sessionId, 1L), row(sessionId, 2L));

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
        UUID sessionId = UUID.randomUUID();
        ImportSession session = session(sessionId);
        ImportRow importedRow = row(sessionId, 1L);
        ImportRow pendingRow = row(sessionId, 2L);
        List<ImportRow> rows = List.of(importedRow, pendingRow);

        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(importRowRepository.findBySessionId(sessionId)).thenReturn(rows);
        when(handler.commit(session, rows)).thenAnswer(invocation -> {
            importedRow.setStatus(ImportRowStatus.IMPORTED);
            throw new RejectedExecutionException("mail queue full");
        });

        assertThatCode(() -> service().commit(sessionId)).doesNotThrowAnyException();

        // Rows that were processed before the error must be saved, and their state must not be
        // lost.
        assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.FAILED);
        assertThat(session.getFailureReason()).contains("mail queue full");
        assertThat(importedRow.getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        verify(importRowRepository).saveAll(rows);
        verify(importSessionRepository).save(session);
    }

    @Test
    void should_not_touch_rows_when_session_type_has_no_handler() {
        UUID sessionId = UUID.randomUUID();
        ImportSession session = session(sessionId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        ImportCommitService serviceWithoutHandler = new ImportCommitService(List.of(), importSessionRepository, importRowRepository, auditActorScopePort);

        assertThatCode(() -> serviceWithoutHandler.commit(sessionId)).isInstanceOf(IllegalStateException.class);
        verify(importRowRepository, never()).saveAll(any());
    }

    @Test
    void should_run_handler_as_the_user_who_started_the_import() {
        UUID sessionId = UUID.randomUUID();
        ImportSession session = session(sessionId);
        List<ImportRow> rows = List.of(row(sessionId, 1L));
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(importRowRepository.findBySessionId(sessionId)).thenReturn(rows);
        when(handler.commit(session, rows)).thenReturn(new ImportCommitResult(1L, 0L, 0L, 0L));

        service().commit(sessionId);

        verify(auditActorScopePort).callAs(eq(String.valueOf(session.getCreatedBy())), any());
    }

    private ImportCommitService service() {
        return new ImportCommitService(List.of(handler), importSessionRepository, importRowRepository, auditActorScopePort);
    }

    private ImportSession session(UUID id) {
        return ImportSession.builder()
            .id(id)
            .schoolId(UUID.randomUUID())
            .type(ImportType.USER)
            .fileName("users.csv")
            .originalHeadersJson("[]")
            .suggestedMappingJson("{}")
            .confirmedMappingJson("{}")
            .validRows(0L)
            .invalidRows(0L)
            .importedRows(0L)
            .skippedRows(0L)
            .totalRows(0L)
            .failureReason(null)
            .status(ImportSessionStatus.IMPORTING)
            .importedEntityId(null)
            .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
            .claimedAt(null)
            .claimedBy(null)
            .leaseExpiresAt(null)
            .attempts(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID().toString())
            .updatedBy(UUID.randomUUID().toString())
            .build();
    }

    private ImportRow row(UUID sessionId, long rowNumber) {
        return ImportRow.builder()
            .id(UUID.randomUUID())
            .sessionId(sessionId)
            .rowNumber(rowNumber)
            .rawDataJson("{}")
            .mappedDataJson(null)
            .errorsJson(null)
            .status(ImportRowStatus.PENDING)
            .build();
    }
}
