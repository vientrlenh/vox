package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.outbox.OutboxJpaEntity;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.outbox.OutboxStatus;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.infrastructure.persistence.mapper.OutboxMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataOutboxRepository;

@Repository
@RequiredArgsConstructor
public class OutboxRepositoryImpl implements OutboxRepository {

    private static final int MAX_RETRIES = 5;
    private static final Duration BASE_BACKOFF = Duration.ofSeconds(30);
    private static final Duration LEASE = Duration.ofMinutes(5);

    private final SpringDataOutboxRepository springDataOutboxRepository;

    @Override
    public Outbox save(Outbox outbox) {
        OutboxJpaEntity entity = OutboxMapper.toJpa(outbox);
        OutboxJpaEntity saved = springDataOutboxRepository.save(entity);
        return OutboxMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public List<Outbox> claimPendingEvents(int size) {
        Instant now = Instant.now();
        List<OutboxJpaEntity> rows = springDataOutboxRepository.lockPendingEvents(now, size);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Outbox> events = rows.stream().map(OutboxMapper::toDomain).toList();
        List<UUID> ids = events.stream().map(e -> e.getId()).toList();
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
        int nextRetryCount = currentRetryCount + 1;
        if (nextRetryCount >= MAX_RETRIES) {
            springDataOutboxRepository.markFailed(id, OutboxStatus.FAILED.name(), lastError, null);
            return;
        }

        Duration backoff = BASE_BACKOFF.multipliedBy(1L << currentRetryCount); // 30s, 60s, 120s
        springDataOutboxRepository.markFailed(id, OutboxStatus.PENDING.name(), lastError, Instant.now().plus(backoff));
    }

    @Override
    @Transactional
    public int releaseExpiredLeases() {
        return springDataOutboxRepository.releaseExpiredLeases(Instant.now());
    }

    
}
