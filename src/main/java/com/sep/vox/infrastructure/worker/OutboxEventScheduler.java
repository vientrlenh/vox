package com.sep.vox.infrastructure.worker;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.EventMessagePublisherPort;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.OutboxRepository;

/**
 * Pumps the outbox out to Kafka.
 *
 * <p>Can be turned off with {@code app.outbox.scheduler.enabled=false}, and the only place that turns it off is the
 * test environment: this job runs every second, so with a developer machine's real broker a single
 * test that writes an outbox row lets a fake event into the production topic. It stays on by default so that
 * every environment running for real has nothing extra to declare.
 */
@Component
@ConditionalOnProperty(name = "app.outbox.scheduler.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class OutboxEventScheduler {

    private static final int BATCH_SIZE = 100;

    private final EventMessagePublisherPort eventMessagePublisherPort;
    private final OutboxRepository outboxRepository;

    @Scheduled(fixedDelay = 1000, scheduler = "outboxTaskScheduler")
    public void publishPendingEvents() {
        List<Outbox> events = claimPendingEvents();
        if (events.isEmpty()) {
            return;
        }
        publishAndMark(events);
    }

    /** @return the claimed events, or an empty list when the claim itself failed (the failure is logged). */
    private List<Outbox> claimPendingEvents() {
        try {
            return outboxRepository.claimPendingEvents(BATCH_SIZE);
        } catch (Exception e) {
            log.error("Outbox claim failed", e);
            return List.of();
        }
    }

    private void publishAndMark(List<Outbox> events) {
        List<Map.Entry<Outbox, CompletableFuture<Void>>> inFlight = events
            .stream()
            .map(event -> Map.entry(event, eventMessagePublisherPort.publish(event)))
            .toList();

        for (Map.Entry<Outbox, CompletableFuture<Void>> entry : inFlight) {
            Outbox event = entry.getKey();
            try {
                entry.getValue().join();
                outboxRepository.markPublished(event.getId(), Instant.now());
            } catch (Exception e) {
                log.error("Outbox event publish failed: eventId={}, eventType={}", event.getId(), event.getEventType(), e);
                outboxRepository.markFailed(event.getId(), describe(e), event.getRetryCount());
            }
        }
    }

    @Scheduled(fixedDelay = 60_000, scheduler = "outboxTaskScheduler")
    public void releaseStuckEvents() {
        try {
            int released = outboxRepository.releaseExpiredLeases();
            if (released > 0) {
                log.warn("Released {} expired outbox events to PENDING", released);
            }
        } catch (Exception e) {
            log.error("Clean lease outbox failed", e);
        }
    }


    private String describe(Exception e) {
        Throwable cause = e.getCause() == null ? e : e.getCause();
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
