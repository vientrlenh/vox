package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
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

    @Override
    public Optional<Notification> saveIfAbsent(Notification notification) {
        if (notification.getEventId() != null
                && springDataNotificationRepository.existsByUserIdAndEventId(
                    notification.getUserId(), notification.getEventId())) {
            return Optional.empty();
        }

        try {
            return Optional.of(save(notification));
        } catch (DataIntegrityViolationException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Notification> findByUserIdOrderByIdDesc(UUID userId, int limit) {
        var pageable = PageRequest.of(0, limit + 1);
        return springDataNotificationRepository.findByUserIdOrderByIdDesc(userId, pageable).stream()
            .map(NotificationMapper::toDomain)
            .toList();
    }

    @Override
    public List<Notification> findByUserIdAndIdLessThanOrderByIdDesc(UUID userId, UUID cursor, int limit) {
        var pageable = PageRequest.of(0, limit + 1);
        return springDataNotificationRepository
            .findByUserIdAndIdLessThanOrderByIdDesc(userId, cursor, pageable).stream()
            .map(NotificationMapper::toDomain)
            .toList();
    }

    @Override
    public long countUnreadByUserId(UUID userId) {
        return springDataNotificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    public int markRead(UUID userId, UUID notificationId, Instant now) {
        return springDataNotificationRepository.markRead(userId, notificationId, now);
    }

    @Override
    public int markAllRead(UUID userId, Instant now) {
        return springDataNotificationRepository.markAllRead(userId, now);
    }

    @Override
    public Optional<Notification> findByIdAndUserId(UUID id, UUID userId) {
        return springDataNotificationRepository.findByIdAndUserId(id, userId)
            .map(NotificationMapper::toDomain);
    }
}
