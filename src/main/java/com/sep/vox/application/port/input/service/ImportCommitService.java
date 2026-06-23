package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;

@Service
public class ImportCommitService {

    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;

    public ImportCommitService(ImportSessionRepository importSessionRepository, ImportRowRepository importRowRepository) {
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
    }
    
    public void commit(UUID sessionId) {
        var session = importSessionRepository.findById(sessionId).orElseThrow(() -> new NotFoundException("Không tìm thấy phiên import yêu cầu"));
        var rows = importRowRepository.findBySessionId(sessionId, null, null);
    }
}
