package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.importfile.ImportSession;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.AuditActorScopePort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;

@Service
public class ImportCommitService {

    private static final Logger log = LoggerFactory.getLogger(ImportCommitService.class);

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
        ImportSession session = importSessionRepository.findById(sessionId).orElseThrow(() -> new NotFoundException("Không tìm thấy phiên import yêu cầu"));
        ImportCommitHandler handler = handlers.get(session.getType());
        if (handler == null) throw new IllegalStateException("Thiếu handler cho " + session.getType());

        List<ImportRow> rows = importRowRepository.findBySessionId(sessionId);
        try {
            ImportCommitResult result = auditActorScopePort.callAs(session.getCreatedBy(), () -> handler.commit(session, rows));
            session.setImportedRows(result.created());
            session.setSkippedRows(result.skipped());
            session.setInvalidRows(result.invalid());
            session.setValidRows(result.created() + result.updated() + result.skipped());
            session.setTotalRows(rows.size());
            session.setStatus(ImportSessionStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý phiên import {} (type={})", sessionId, session.getType(), e);
            session.setStatus(ImportSessionStatus.FAILED);
            session.setFailureReason(
                e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
            );
        }
        // Save the row state on BOTH branches: the handler processes each row in its own
        // transaction, so when it fails midway the rows already done must keep their result
        // instead of all going back to PENDING.
        saveRowsQuietly(rows);
        importSessionRepository.save(session);

    }

    private void saveRowsQuietly(List<ImportRow> rows) {
        try {
            importRowRepository.saveAll(rows);
        } catch (Exception e) {
            log.error("Không lưu được trạng thái các dòng import", e);
        }
    }

}
