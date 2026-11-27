package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.importfile.ImportSession;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.AuditActorScopePort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;

@Service
@Slf4j
public class ImportCommitService {

    private final Map<ImportType, ImportCommitHandler> handlers;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final AuditActorScopePort auditActorScopePort;

    public ImportCommitService(List<ImportCommitHandler> handlerList, ImportSessionRepository importSessionRepository, ImportRowRepository importRowRepository, AuditActorScopePort auditActorScopePort) {
        this.handlers = handlerList.stream()
            .collect(Collectors.toMap(h -> h.supportedType(), h -> h));
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.auditActorScopePort = auditActorScopePort;
    }
    
    public void commit(UUID sessionId) {
        ImportSession session = importSessionRepository.findById(sessionId).orElseThrow(() -> new ResourceNotFoundException("Requested import session not found"));
        ImportCommitHandler handler = handlers.get(session.getType());
        if (handler == null) throw new IllegalStateException("Missing handler for " + session.getType());

        List<ImportRow> rows = importRowRepository.findBySessionId(sessionId);
        try {
            ImportCommitResult result = auditActorScopePort.callAs(session.getCreatedBy(), () -> handler.commit(session, rows));
            recordCompleted(session, result, rows.size());
        } catch (Exception e) {
            recordFailed(session, e);
        }
        // Save the row state on BOTH branches: the handler processes each row in its own
        // transaction, so when it fails midway the rows already done must keep their result
        // instead of all going back to PENDING.
        saveRowsQuietly(rows);
        importSessionRepository.save(session);
    }

    private void recordCompleted(ImportSession session, ImportCommitResult result, int totalRows) {
        session.setImportedRows(result.created());
        session.setSkippedRows(result.skipped());
        session.setInvalidRows(result.invalid());
        session.setValidRows(result.created() + result.updated() + result.skipped());
        session.setTotalRows(totalRows);
        session.setStatus(ImportSessionStatus.COMPLETED);
    }

    private void recordFailed(ImportSession session, Exception e) {
        log.error("Error while processing import session {} (type={})", session.getId(), session.getType(), e);
        session.setStatus(ImportSessionStatus.FAILED);
        session.setFailureReason(
            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
        );
    }

    private void saveRowsQuietly(List<ImportRow> rows) {
        try {
            importRowRepository.saveAll(rows);
        } catch (Exception e) {
            log.error("Could not save the status of the import rows", e);
        }
    }

}
