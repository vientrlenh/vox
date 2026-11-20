package com.sep.vox.application.port.input.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.ImportSessionRepository;

@Service
public class ImportQueueService {
    private final ImportSessionRepository importSessionRepository;

    public ImportQueueService(ImportSessionRepository importSessionRepository) {
        this.importSessionRepository = importSessionRepository;
    }

    @Transactional
    public List<UUID> claimQueued(UUID worker, int limit, Duration lease) {
        var now = Instant.now();
        var ids = importSessionRepository.lockQueueIds(limit);
        if (!ids.isEmpty()) {
            importSessionRepository.markClaimed(ids, worker, now, now.plus(lease));
        }
        return ids;
    }

    @Transactional
    public void heartbeat(UUID sessionId, Duration lease) {
        importSessionRepository.extendLease(sessionId, Instant.now().plus(lease));
    }

    @Transactional
    public int requeueExpired(int maxAttempts) {
        return importSessionRepository.requeueExpiredLeases(Instant.now(), maxAttempts);
    }
}
