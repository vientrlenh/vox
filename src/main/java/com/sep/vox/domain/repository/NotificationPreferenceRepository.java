package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.notification.NotificationCategory;
import com.sep.vox.domain.model.notification.NotificationPreference;

public interface NotificationPreferenceRepository {
    Optional<NotificationPreference> findById(UUID id);
    NotificationPreference save(NotificationPreference preference);

    /**
     * @return empty when the user has never changed the setting for this group -- the caller must
     *         read that as "use the default", see NotificationPreference#DEFAULT_PUSH_ENABLED
     */
    Optional<NotificationPreference> findByUserIdAndCategory(UUID userId, NotificationCategory category);
}
