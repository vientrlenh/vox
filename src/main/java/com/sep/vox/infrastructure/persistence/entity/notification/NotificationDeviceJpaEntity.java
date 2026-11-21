package com.sep.vox.infrastructure.persistence.entity.notification;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "notification_devices", indexes = {
    @Index(columnList = "user_id", name = "idx_notification_devices_user")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = "installation_id", name = "uk_notification_devices_installation_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDeviceJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "platform", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_notification_devices_platform_valid",
            constraint = "platform IN ('WEB', 'ANDROID', 'IOS')"
        )
    })
    private String platform;

    @Column(name = "installation_id", nullable = false, length = 50)
    private String installationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;
}
