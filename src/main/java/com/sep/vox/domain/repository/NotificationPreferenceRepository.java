package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.notification.NotificationPreference;

public interface NotificationPreferenceRepository {
    Optional<NotificationPreference> findById(UUID id);
    NotificationPreference save(NotificationPreference preference);
}
