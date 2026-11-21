package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.notification.NotificationCategory;
import com.sep.vox.domain.model.notification.NotificationPreference;
import com.sep.vox.infrastructure.persistence.entity.notification.NotificationPreferenceJpaEntity;

public final class NotificationPreferenceMapper {

    private NotificationPreferenceMapper() {}

    public static NotificationPreference toDomain(NotificationPreferenceJpaEntity jpa) {
        return NotificationPreference.builder()
                .id(jpa.getId())
                .userId(jpa.getUserId())
                .category(categoryFromString(jpa.getCategory()))
                .pushEnabled(jpa.isPushEnabled())
                .emailEnabled(jpa.isEmailEnabled())
                .updatedAt(jpa.getUpdatedAt())
                .build();
    }

    public static NotificationPreferenceJpaEntity toJpa(NotificationPreference preference) {
        return NotificationPreferenceJpaEntity.builder()
                .id(preference.getId())
                .userId(preference.getUserId())
                .category(valueOf(preference.getCategory()))
                .pushEnabled(preference.isPushEnabled())
                .emailEnabled(preference.isEmailEnabled())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    private static NotificationCategory categoryFromString(String category) {
        return category == null ? null : NotificationCategory.valueOf(category);
    }

    private static String valueOf(NotificationCategory category) {
        return category == null ? null : category.name();
    }
}
