package com.sep.vox.infrastructure.persistence.entity.notification;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Stores only what DIFFERS from the default. A missing row means "use the default", see
 * NotificationPreference.DEFAULT_PUSH_ENABLED / DEFAULT_EMAIL_ENABLED.
 */
@Entity
@Table(name = "notification_preferences", uniqueConstraints = {
    @UniqueConstraint(
        columnNames = {"user_id", "category"},
        name = "uk_notification_preferences_user_category"
    )
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreferenceJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "category", nullable = false, length = 50, updatable = false, check = {
        @CheckConstraint(
            name = "chk_notification_preferences_category_valid",
            constraint = "category IN ('ASSESSMENT_RESULT', 'ASSESSMENT_APPEAL', 'GRADING', 'ASSESSMENT_SCHEDULE', 'SYSTEM')"
        )
    })
    private String category;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
