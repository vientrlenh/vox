package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.DeviceSessionJpaEntity;


public interface SpringDataDeviceSessionRepository extends JpaRepository<DeviceSessionJpaEntity, UUID>{
    List<DeviceSessionJpaEntity> findByUserId(UUID userId);
}
