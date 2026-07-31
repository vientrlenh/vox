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
 * Tiện ích cho các test của use case đã chuyển từ {@code EventPublisherPort} sang outbox.
 *
 * <p>Dùng adapter JSON THẬT chứ không mock: mock trả {@code null} thì cột payload rỗng và
 * test sẽ xanh kể cả khi use case dựng sai payload — đúng thứ cần bắt được ở đây.
 */
public final class OutboxTestSupport {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private OutboxTestSupport() {}

    public static JsonSerializationPort jsonSerializationPort() {
        return new JacksonJsonSerializationService(JSON_MAPPER);
    }

    /** Dòng outbox đầu tiên có đúng {@code eventType}; ném {@link AssertionError} nếu không có. */
    public static Outbox captureOutbox(OutboxRepository outboxRepository, String eventType) {
        var captor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream()
            .filter(outbox -> eventType.equals(outbox.getEventType()))
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "Không có outbox nào với eventType=" + eventType
                    + ", đã ghi: " + captor.getAllValues().stream().map(outbox -> outbox.getEventType()).toList()));
    }

    /** Giải payload của dòng outbox có đúng {@code eventType} về record tương ứng. */
    public static <T> T capturePayload(OutboxRepository outboxRepository, String eventType, Class<T> type) {
        return JSON_MAPPER.readValue(captureOutbox(outboxRepository, eventType).getPayload(), type);
    }
}
