package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.notification.Notification;
import com.sep.vox.domain.repository.NotificationRepository;
import com.sep.vox.infrastructure.persistence.mapper.NotificationMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataNotificationRepository;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

    private final SpringDataNotificationRepository springDataNotificationRepository;

    public NotificationRepositoryImpl(SpringDataNotificationRepository springDataNotificationRepository) {
        this.springDataNotificationRepository = springDataNotificationRepository;
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return springDataNotificationRepository.findById(id)
            .map(NotificationMapper::toDomain);
    }

    @Override
    public Notification save(Notification notification) {
        var entity = NotificationMapper.toJpa(notification);
        var saved = springDataNotificationRepository.save(entity);
        return NotificationMapper.toDomain(saved);
    }
    
}
