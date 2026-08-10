package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.NotificationPreferenceJpaEntity;

public interface SpringDataNotificationPreferenceRepository extends JpaRepository<NotificationPreferenceJpaEntity, UUID> {

    Optional<NotificationPreferenceJpaEntity> findByUserIdAndCategory(UUID userId, String category);
}
