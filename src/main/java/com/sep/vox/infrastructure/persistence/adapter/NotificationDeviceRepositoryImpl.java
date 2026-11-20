package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.notification.NotificationDevice;
import com.sep.vox.domain.model.notification.NotificationDevicePlatform;
import com.sep.vox.domain.repository.NotificationDeviceRepository;
import com.sep.vox.infrastructure.persistence.mapper.NotificationDeviceMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataNotificationDeviceRepository;

@Repository
public class NotificationDeviceRepositoryImpl implements NotificationDeviceRepository {

    private final SpringDataNotificationDeviceRepository springDataNotificationDeviceRepository;

    public NotificationDeviceRepositoryImpl(
            SpringDataNotificationDeviceRepository springDataNotificationDeviceRepository) {
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

    @Override
    public List<NotificationDevice> findByUserId(UUID userId) {
        return springDataNotificationDeviceRepository.findByUserId(userId).stream()
            .map(NotificationDeviceMapper::toDomain)
            .toList();
    }

    /** {@code @Modifying} cần transaction; consumer gọi ngoài transaction nên mở ở đây. */
    @Override
    @Transactional
    public int deleteByInstallationIdIn(Collection<String> installationIds) {
        if (installationIds == null || installationIds.isEmpty()) {
            return 0;
        }
        return springDataNotificationDeviceRepository.deleteByInstallationIdIn(installationIds);
    }

    @Override
    public int deleteByUserIdAndDeviceIdAndExceptInstallationId(UUID userId, String deviceId, String installationId) {
        return springDataNotificationDeviceRepository.deleteByUserIdAndDeviceIdAndExceptInstallationId(userId, deviceId, installationId);
    }

    @Override
    public int deleteByUserIdAndInstallationId(UUID userId, String installationId) {
        return springDataNotificationDeviceRepository.deleteByUserIdAndInstallationId(userId, installationId);
    }

    @Override
    @Transactional
    public int deleteByUserIdAndDeviceId(UUID userId, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return 0;
        }
        return springDataNotificationDeviceRepository.deleteByUserIdAndDeviceId(userId, deviceId);
    }

    @Override
    @Transactional
    public int deleteByLastSeenAtBefore(Instant threshold) {
        return springDataNotificationDeviceRepository.deleteByLastSeenAtBefore(threshold);
    }

    @Override
    public int registerDevice(UUID userId, String deviceId, NotificationDevicePlatform platform, String installationId, Instant now) {
        return springDataNotificationDeviceRepository.registerDevice(userId, deviceId, platform.name(), installationId, now);
    }

    @Override
    public boolean existByUserIdAndDeviceId(UUID userId, String deviceId) {
        return springDataNotificationDeviceRepository.existsByUserIdAndDeviceId(userId, deviceId);
    }

    @Override
    public Optional<NotificationDevice> findFirstByUserIdAndDeviceIdOrderByLastSeenAtDesc(UUID userId, String deviceId) {
        return springDataNotificationDeviceRepository.findFirstByUserIdAndDeviceIdOrderByLastSeenAtDesc(userId, deviceId)
            .map(NotificationDeviceMapper::toDomain);
    }
}
