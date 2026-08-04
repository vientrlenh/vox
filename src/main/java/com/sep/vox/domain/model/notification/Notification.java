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

    public Notification(UUID id, UUID userId, UUID eventId, String eventType, String title, String body, String payload,
            Instant readAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.eventType = eventType;
        this.title = title;
        this.body = body;
        this.payload = payload;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    public Notification(UUID userId, UUID eventId, String eventType, String title, String body, String payload,
            Instant readAt, Instant createdAt) {
        this.userId = userId;
        this.eventId = eventId;
        this.eventType = eventType;
        this.title = title;
        this.body = body;
        this.payload = payload;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

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

    
    
}
