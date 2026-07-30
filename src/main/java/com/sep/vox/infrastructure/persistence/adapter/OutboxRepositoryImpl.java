package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.outbox.OutboxStatus;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.infrastructure.persistence.mapper.OutboxMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataOutboxRepository;

@Repository
public class OutboxRepositoryImpl implements OutboxRepository {

    private static final int MAX_RETRIES = 5;
    private static final Duration BASE_BACKOFF = Duration.ofSeconds(30);
    private static final Duration LEASE = Duration.ofMinutes(5);

    private final SpringDataOutboxRepository springDataOutboxRepository;

    public OutboxRepositoryImpl(SpringDataOutboxRepository springDataOutboxRepository) {
        this.springDataOutboxRepository = springDataOutboxRepository;
    }

    @Override
    public Outbox save(Outbox outbox) {
        var entity = OutboxMapper.toJpa(outbox);
        var saved = springDataOutboxRepository.save(entity);
        return OutboxMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public List<Outbox> claimPendingEvents(int size) {
        var now = Instant.now();
        var rows = springDataOutboxRepository.lockPendingEvents(now, size);
        if (rows.isEmpty()) {
            return List.of();
        }
        var events = rows.stream().map(OutboxMapper::toDomain).toList();
        var ids = events.stream().map(e -> e.getId()).toList();
        springDataOutboxRepository.markProcessing(ids, now.plus(LEASE));
        return events;
    }

    @Override
    @Transactional
    public void markPublished(UUID id, Instant publishedAt) {
        springDataOutboxRepository.markPublished(id, publishedAt);
    }

    @Override
    @Transactional
    public void markFailed(UUID id, String lastError, int currentRetryCount) {
        var nextRetryCount = currentRetryCount + 1;
        if (nextRetryCount >= MAX_RETRIES) {
            springDataOutboxRepository.markFailed(id, OutboxStatus.FAILED.name(), lastError, null);
            return;
        }

        var backoff = BASE_BACKOFF.multipliedBy(1L << currentRetryCount); // 30s, 60s, 120s
        springDataOutboxRepository.markFailed(id, OutboxStatus.PENDING.name(), lastError, Instant.now().plus(backoff));
    }

    @Override
    @Transactional
    public int releaseExpiredLeases() {
        return springDataOutboxRepository.releaseExpiredLeases(Instant.now());
    }

    
}
