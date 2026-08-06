package com.sep.vox.domain.model.notification;

import java.time.Instant;
import java.util.UUID;

public class NotificationPreference {
    private UUID id;
    private UUID userId;
    private NotificationCategory category;
    private boolean pushEnabled;
    private boolean emailEnabled;
    private Instant updatedAt;
    
    public NotificationPreference() {}

    public NotificationPreference(UUID id, UUID userId, NotificationCategory category, boolean pushEnabled,
            boolean emailEnabled, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.pushEnabled = pushEnabled;
        this.emailEnabled = emailEnabled;
        this.updatedAt = updatedAt;
    }

    public NotificationPreference(UUID userId, NotificationCategory category, boolean pushEnabled,
            boolean emailEnabled, Instant updatedAt) {
        this.userId = userId;
        this.category = category;
        this.pushEnabled = pushEnabled;
        this.emailEnabled = emailEnabled;
        this.updatedAt = updatedAt;
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

    public NotificationCategory getCategory() {
        return category;
    }

    public void setCategory(NotificationCategory category) {
        this.category = category;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    
}
