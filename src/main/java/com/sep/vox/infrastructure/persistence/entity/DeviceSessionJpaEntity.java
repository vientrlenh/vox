package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.sep.vox.infrastructure.shared.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "device_sessions", indexes = {
    @Index(columnList = "user_id", name = "idx_device_sessions_users")
})
@Getter 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder 
public class DeviceSessionJpaEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "device_id", nullable = false, updatable = false, length = 255)
    private String deviceId;

    @Column(name = "device_name", nullable = false, updatable = false, length = 255)
    private String deviceName;

    @Column(name = "platform", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_device_sessions_platform_valid", 
            constraint = "platform IN ('WEB', 'ANDROID', 'IOS', 'DESKTOP')"
        )
    })
    private String platform;

    @Column(name = "ip_address", nullable = false, updatable = false, length = 255)
    private String ipAddress;

    @Column(name = "user_agent", updatable = false, length = 255)
    private String userAgent;

    @Column(name = "revoked_at")
    private Instant revokedAt;

}
