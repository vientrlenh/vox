package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.notification.NotificationDevice;
import com.sep.vox.domain.repository.NotificationDeviceRepository;
import com.sep.vox.infrastructure.persistence.mapper.NotificationDeviceMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataNotificationDeviceRepository;

@Repository
public class NotificationDeviceRepositoryImpl implements NotificationDeviceRepository {
    
    private final SpringDataNotificationDeviceRepository springDataNotificationDeviceRepository;

    public NotificationDeviceRepositoryImpl(SpringDataNotificationDeviceRepository springDataNotificationDeviceRepository) {
        this.springDataNotificationDeviceRepository = springDataNotificationDeviceRepository;
    }

    @Override
    public Optional<NotificationDevice> findById(UUID id) {
        return springDataNotificationDeviceRepository.findById(id)
            .map(NotificationDeviceMapper::toDomain);
    }

    @Override
    public NotificationDevice save(NotificationDevice notificationDevice) {
        var entity = NotificationDeviceMapper.toJpa(notificationDevice);
        var saved = springDataNotificationDeviceRepository.save(entity);
        return NotificationDeviceMapper.toDomain(saved);
    }

    
}
