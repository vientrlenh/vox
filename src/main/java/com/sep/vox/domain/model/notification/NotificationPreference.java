package com.sep.vox.domain.model.notification;

import java.time.Instant;
import java.util.UUID;

public class NotificationPreference {

    /**
     * Không có dòng trong notification_preferences nghĩa là "dùng mặc định". Hai hằng số
     * này là nguồn duy nhất của giá trị mặc định đó, để phía Java và câu
     * {@code COALESCE(np.push_enabled, TRUE)} phía SQL không bao giờ lệch nhau.
     */
    public static final boolean DEFAULT_PUSH_ENABLED = true;
    public static final boolean DEFAULT_EMAIL_ENABLED = true;

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
