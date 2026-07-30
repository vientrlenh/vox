package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.outbox.Outbox;

public interface OutboxRepository {
    Outbox save(Outbox outbox);
    List<Outbox> claimPendingEvents(int size);
    void markPublished(UUID id, Instant publishedAt);
    void markFailed(UUID id, String lastError, int currentRetryCount);
    int releaseExpiredLeases();

}
