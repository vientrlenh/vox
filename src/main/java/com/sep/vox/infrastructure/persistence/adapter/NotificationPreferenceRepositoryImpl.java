package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.notification.NotificationCategory;
import com.sep.vox.domain.model.notification.NotificationPreference;
import com.sep.vox.domain.repository.NotificationPreferenceRepository;
import com.sep.vox.infrastructure.persistence.mapper.NotificationPreferenceMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataNotificationPreferenceRepository;

@Repository
public class NotificationPreferenceRepositoryImpl implements NotificationPreferenceRepository {

    private final SpringDataNotificationPreferenceRepository springDataNotificationPreferenceRepository;

    public NotificationPreferenceRepositoryImpl(
            SpringDataNotificationPreferenceRepository springDataNotificationPreferenceRepository) {
        this.springDataNotificationPreferenceRepository = springDataNotificationPreferenceRepository;
    }

    @Override
    public Optional<NotificationPreference> findById(UUID id) {
        return springDataNotificationPreferenceRepository.findById(id)
            .map(NotificationPreferenceMapper::toDomain);
    }

    @Override
    public NotificationPreference save(NotificationPreference preference) {
        var entity = NotificationPreferenceMapper.toJpa(preference);
        var saved = springDataNotificationPreferenceRepository.save(entity);
        return NotificationPreferenceMapper.toDomain(saved);
    }

    @Override
    public Optional<NotificationPreference> findByUserIdAndCategory(UUID userId, NotificationCategory category) {
        if (category == null) {
            return Optional.empty();
        }
        return springDataNotificationPreferenceRepository
            .findByUserIdAndCategory(userId, category.name())
            .map(NotificationPreferenceMapper::toDomain);
    }
}
