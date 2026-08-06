package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.notification.NotificationCategory;
import com.sep.vox.domain.model.notification.NotificationPreference;
import com.sep.vox.infrastructure.persistence.entity.NotificationPreferenceJpaEntity;

public final class NotificationPreferenceMapper {

    private NotificationPreferenceMapper() {}

    public static NotificationPreference toDomain(NotificationPreferenceJpaEntity jpa) {
        return new NotificationPreference(
            jpa.getId(),
            jpa.getUserId(),
            categoryFromString(jpa.getCategory()),
            jpa.isPushEnabled(),
            jpa.isEmailEnabled(),
            jpa.getUpdatedAt()
        );
    }

    public static NotificationPreferenceJpaEntity toJpa(NotificationPreference preference) {
        return new NotificationPreferenceJpaEntity(
            preference.getId(),
            preference.getUserId(),
            valueOf(preference.getCategory()),
            preference.isPushEnabled(),
            preference.isEmailEnabled(),
            preference.getUpdatedAt()
        );
    }

    private static NotificationCategory categoryFromString(String category) {
        return category == null ? null : NotificationCategory.valueOf(category);
    }

    private static String valueOf(NotificationCategory category) {
        return category == null ? null : category.name();
    }
}
