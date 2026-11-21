package com.sep.vox.domain.model.notification;

import java.time.Instant;
import java.util.UUID;

public class Notification {
    private UUID id;
    private UUID userId;
    private UUID eventId;
    private String eventType;
    private String title;
    private String body;
    private String payload;
    private Instant readAt;
    private Instant createdAt;

    public Notification() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    
    

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Notification target = new Notification();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder userId(UUID userId) {
            target.userId = userId;
            return this;
        }

        public Builder eventId(UUID eventId) {
            target.eventId = eventId;
            return this;
        }

        public Builder eventType(String eventType) {
            target.eventType = eventType;
            return this;
        }

        public Builder title(String title) {
            target.title = title;
            return this;
        }

        public Builder body(String body) {
            target.body = body;
            return this;
        }

        public Builder payload(String payload) {
            target.payload = payload;
            return this;
        }

        public Builder readAt(Instant readAt) {
            target.readAt = readAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public Notification build() {
            return target;
        }

    }
}
