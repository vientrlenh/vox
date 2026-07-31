package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
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

    public ImportCommitService(List<ImportCommitHandler> handlerList, ImportSessionRepository importSessionRepository, ImportRowRepository importRowRepository) {
        this.handlers = handlerList.stream()
            .collect(Collectors.toMap(h -> h.supportedType(), h -> h));
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
    }
    
    public void commit(UUID sessionId) {
        var session = importSessionRepository.findById(sessionId).orElseThrow(() -> new NotFoundException("Không tìm thấy phiên import yêu cầu"));
        var handler = handlers.get(session.getType());
        if (handler == null) throw new IllegalStateException("Thiếu handler cho " + session.getType());

        var rows = importRowRepository.findBySessionId(sessionId);
        try {
            var result = handler.commit(session, rows);
            importRowRepository.saveAll(rows);
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
        session.setUpdatedAt(Instant.now());
        importSessionRepository.save(session);

    }

}
