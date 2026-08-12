package com.sep.vox.domain.model.personalization;

import java.time.Instant;
import java.util.UUID;

public class TopicInterestEvent {

    private UUID topicId;
    private UUID sessionId;
    /**
     * {@code SESSION_OUTCOME} (học sinh ĐÃ luyện chủ đề này) hoặc {@code OFFERED_NOT_CHOSEN}
     * (chỉ được chào rồi bỏ qua).
     *
     * <p>Trước đây cột này có trong bảng nhưng KHÔNG được đọc lên domain, nên
     * {@code recomputeInterest} coi mọi sự kiện như nhau -- kể cả khi tính "lần cuối chạm chủ
     * đề này" và "đã luyện qua bao nhiêu phiên". Hai đại lượng đó chỉ đúng với sự kiện ĐÃ
     * LUYỆN; tính cả sự kiện bị bỏ qua thì chủ đề chưa từng luyện vẫn bị phạt như vừa luyện
     * xong. Xem chú thích ở {@code InterestVectorService.recomputeInterest}.
     */
    private String eventType;
    private double signal;
    private Instant occurredAt;

    public TopicInterestEvent() {
    }

    public TopicInterestEvent(
            UUID topicId,
            UUID sessionId,
            String eventType,
            double signal,
            Instant occurredAt) {
        this.topicId = topicId;
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.signal = signal;
        this.occurredAt = occurredAt;
    }

    /** Sự kiện này có phải "học sinh đã thật sự luyện chủ đề" không. */
    public boolean isSessionOutcome() {
        return "SESSION_OUTCOME".equals(eventType);
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public void setTopicId(UUID topicId) {
        this.topicId = topicId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public double getSignal() {
        return signal;
    }

    public void setSignal(double signal) {
        this.signal = signal;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
