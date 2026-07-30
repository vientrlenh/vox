package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.DeviceSessionJpaEntity;


public interface SpringDataDeviceSessionRepository extends JpaRepository<DeviceSessionJpaEntity, UUID>{
    List<DeviceSessionJpaEntity> findByUserId(UUID userId);

    @Modifying
    @Query("""
        UPDATE DeviceSessionJpaEntity d 
        SET d.revokedAt = :now 
        WHERE d.id = :id 
            AND d.revokedAt IS NULL
    """)
    int revokeDeviceSession(@Param("id") UUID id, @Param("now") Instant now);
}
