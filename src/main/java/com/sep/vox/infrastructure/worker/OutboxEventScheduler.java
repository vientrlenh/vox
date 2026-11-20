package com.sep.vox.infrastructure.worker;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.EventMessagePublisherPort;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.OutboxRepository;

/**
 * Bơm outbox ra Kafka.
 *
 * <p>Tắt được bằng {@code app.outbox.scheduler.enabled=false}, và chỗ duy nhất tắt nó là
 * môi trường test: job này chạy mỗi giây, nên với broker thật của máy dev thì chỉ cần một
 * test ghi được hàng outbox là một event giả lọt vào topic production. Mặc định vẫn bật để
 * mọi môi trường chạy thật không phải khai thêm gì.
 */
@Component
@ConditionalOnProperty(name = "app.outbox.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxEventScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxEventScheduler.class);
    private static final int BATCH_SIZE = 100;

    private final EventMessagePublisherPort eventMessagePublisherPort;
    private final OutboxRepository outboxRepository;

    public OutboxEventScheduler(
        EventMessagePublisherPort eventMessagePublisherPort, 
        OutboxRepository outboxRepository) {
        this.eventMessagePublisherPort = eventMessagePublisherPort;
        this.outboxRepository = outboxRepository;
    }
    
    @Scheduled(fixedDelay = 1000, scheduler = "outboxTaskScheduler")
    public void publishPendingEvents() {
        List<Outbox> events;
        try {
            events = outboxRepository.claimPendingEvents(BATCH_SIZE);
        } catch (Exception e) {
            LOGGER.error("Outbox claim failed", e);
            return;
        }
        if (events.isEmpty()) {
            return;
        }

        var inFlight = events
            .stream()
            .map(event -> Map.entry(event, eventMessagePublisherPort.publish(event)))
            .toList();
        
        for (var entry : inFlight) {
            var event = entry.getKey();
            try {
                entry.getValue().join();
                outboxRepository.markPublished(event.getId(), Instant.now());
            } catch (Exception e) {
                LOGGER.error("Outbox event publish failed: eventId={}, eventType={}", event.getId(), event.getEventType(), e);
                outboxRepository.markFailed(event.getId(), describe(e), event.getRetryCount());
            }
        }
    }

    @Scheduled(fixedDelay = 60_000, scheduler = "outboxTaskScheduler")
    public void releaseStuckEvents() {
        try {
            var released = outboxRepository.releaseExpiredLeases();
            if (released > 0) {
                LOGGER.warn("Released {} expired outbox events to PENDING", released);
            }
        } catch (Exception e) {
            LOGGER.error("Clean lease outbox failed", e);
        }
    }


    private String describe(Exception e) {
        var cause = e.getCause() == null ? e : e.getCause();
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
