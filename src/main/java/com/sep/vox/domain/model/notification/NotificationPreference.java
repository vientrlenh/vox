package com.sep.vox.domain.model.notification;

import java.time.Instant;
import java.util.UUID;

public class NotificationPreference {

    /**
     * A missing row in notification_preferences means "use the default". These two constants
     * are the only source of that default value, so the Java side and the
     * {@code COALESCE(np.push_enabled, TRUE)} clause on the SQL side can never drift apart.
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

    

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final NotificationPreference target = new NotificationPreference();

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

        public Builder category(NotificationCategory category) {
            target.category = category;
            return this;
        }

        public Builder pushEnabled(boolean pushEnabled) {
            target.pushEnabled = pushEnabled;
            return this;
        }

        public Builder emailEnabled(boolean emailEnabled) {
            target.emailEnabled = emailEnabled;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            target.updatedAt = updatedAt;
            return this;
        }

        public NotificationPreference build() {
            return target;
        }

    }
}
