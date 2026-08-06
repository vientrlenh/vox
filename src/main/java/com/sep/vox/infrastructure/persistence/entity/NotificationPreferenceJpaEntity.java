package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Chỉ lưu phần KHÁC mặc định. Không có dòng nghĩa là "dùng mặc định", xem
 * NotificationPreference.DEFAULT_PUSH_ENABLED / DEFAULT_EMAIL_ENABLED.
 */
@Entity
@Table(name = "notification_preferences", uniqueConstraints = {
    @UniqueConstraint(
        columnNames = {"user_id", "category"},
        name = "uk_notification_preferences_user_category"
    )
})
public class NotificationPreferenceJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "category", nullable = false, length = 50, updatable = false, check = {
        @CheckConstraint(
            name = "chk_notification_preferences_category_valid",
            constraint = "category IN ('EXAM_RESULT', 'EXAM_APPEAL', 'GRADING', 'EXAM_SCHEDULE', 'SYSTEM')"
        )
    })
    private String category;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationPreferenceJpaEntity() {}

    public NotificationPreferenceJpaEntity(UUID id, UUID userId, String category, boolean pushEnabled,
            boolean emailEnabled, Instant updatedAt) {
        this.id = id;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
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
