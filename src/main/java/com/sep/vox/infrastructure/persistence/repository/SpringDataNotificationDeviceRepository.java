package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.NotificationDeviceJpaEntity;

public interface SpringDataNotificationDeviceRepository extends JpaRepository<NotificationDeviceJpaEntity, UUID> {

    List<NotificationDeviceJpaEntity> findByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM NotificationDeviceJpaEntity d WHERE d.installationId IN :installationIds")
    int deleteByInstallationIdIn(@Param("installationIds") Collection<String> installationIds);

    @Modifying
    @Query("""
        DELETE FROM NotificationDeviceJpaEntity d
        WHERE d.userId = :userId 
            AND d.deviceId = :deviceId 
            AND d.installationId <> :installationId
    """)
    int deleteByUserIdAndDeviceIdAndExceptInstallationId(@Param("userId") UUID userId, @Param("deviceId") String deviceId, @Param("installationId") String installationId);


    @Modifying
    @Query("""
        DELETE FROM NotificationDeviceJpaEntity d
        WHERE d.userId = :userId
            AND d.installationId = :installationId
    """)
    int deleteByUserIdAndInstallationId(@Param("userId") UUID userId, @Param("installationId") String installationId);

    @Modifying
    @Query("""
        DELETE FROM NotificationDeviceJpaEntity d
        WHERE d.userId = :userId
            AND d.deviceId = :deviceId
    """)
    int deleteByUserIdAndDeviceId(@Param("userId") UUID userId, @Param("deviceId") String deviceId);

    @Modifying
    @Query("""
        DELETE FROM NotificationDeviceJpaEntity d
        WHERE d.lastSeenAt < :threshold
    """)
    int deleteByLastSeenAtBefore(@Param("threshold") Instant threshold);

    @Modifying
    @Query(value = """
        INSERT INTO notification_devices (user_id, device_id, platform, installation_id, created_at, last_seen_at) VALUES (:userId, :deviceId, :platform, :installationId, :now, :now) 
        ON CONFLICT (installation_id) DO UPDATE 
            SET user_id = excluded.user_id, 
                device_id = excluded.device_id, 
                platform = excluded.platform, 
                last_seen_at = excluded.last_seen_at
    """, nativeQuery = true)
    int registerDevice(@Param("userId") UUID userId, @Param("deviceId") String deviceId, @Param("platform") String platform, @Param("installationId") String installationId, @Param("now") Instant now);
}
