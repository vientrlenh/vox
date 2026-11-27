package com.sep.vox.support;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.infrastructure.service.JacksonJsonSerializationService;

import tools.jackson.databind.json.JsonMapper;

/**
 * Helpers for the tests of use cases that moved from {@code EventPublisherPort} to the outbox.
 *
 * <p>Uses the REAL JSON adapter, not a mock: a mock returns {@code null}, so the payload column
 * would be empty and the test would stay green even when the use case builds a wrong payload,
 * which is exactly what must be caught here.
 */
public final class OutboxTestSupport {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private OutboxTestSupport() {}

    public static JsonSerializationPort jsonSerializationPort() {
        return new JacksonJsonSerializationService(JSON_MAPPER);
    }

    /**
     * The first outbox row with exactly this {@code eventType}; throws {@link AssertionError} if
     * there is none.
     */
    public static Outbox captureOutbox(OutboxRepository outboxRepository, String eventType) {
        ArgumentCaptor<Outbox> captor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream()
            .filter(outbox -> eventType.equals(outbox.getEventType()))
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "No outbox row with eventType=" + eventType
                    + ", written: " + captor.getAllValues().stream().map(outbox -> outbox.getEventType()).toList()));
    }

    /** Deserializes the payload of the outbox row with exactly this {@code eventType} into the given record type. */
    public static <T> T capturePayload(OutboxRepository outboxRepository, String eventType, Class<T> type) {
        return JSON_MAPPER.readValue(captureOutbox(outboxRepository, eventType).getPayload(), type);
    }
}
