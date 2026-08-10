package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.NotificationJpaEntity;

public interface SpringDataNotificationRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    boolean existsByUserIdAndEventId(UUID userId, UUID eventId);

    Slice<NotificationJpaEntity> findByUserIdOrderByIdDesc(UUID userId, Pageable pageable);

    Slice<NotificationJpaEntity> findByUserIdAndIdLessThanOrderByIdDesc(UUID userId, UUID cursor, Pageable pageable);

    long countByUserIdAndReadAtIsNull(UUID userId);

    Optional<NotificationJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE NotificationJpaEntity n 
            SET n.readAt = :now 
        WHERE n.userId = :userId 
            AND n.id = :notificationId 
            AND n.readAt IS NULL    
    """)
    int markRead(@Param("userId") UUID userId, @Param("notificationId") UUID notificationId, @Param("now") Instant now);


    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE NotificationJpaEntity n 
            SET n.readAt = :now 
        WHERE n.userId = :userId 
            AND n.readAt IS NULL 
    """)
    int markAllRead(@Param("userId") UUID userId, @Param("now") Instant now);
}
