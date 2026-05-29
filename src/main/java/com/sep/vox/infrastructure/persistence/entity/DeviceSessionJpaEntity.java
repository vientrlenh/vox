package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "device_sessions", indexes = {
    @Index(columnList = "user_id", name = "idx_device_sessions_users")
})
public class DeviceSessionJpaEntity {
    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        updatable = false,
        nullable = false,
        insertable = false,
        columnDefinition = "UUID default uuidv7()"
    )
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "device_id", nullable = false, updatable = false, length = 255)
    private String deviceId;

    @Column(name = "device_name", nullable = false, updatable = false, length = 255)
    private String deviceName;

    @Column(name = "platform", nullable = false, updatable = false, length = 20)
    private String platform;

    @Column(name = "ip_address", nullable = false, updatable = false, length = 255)
    private String ipAddress;

    @Column(name = "user_agent", updatable = false, length = 255)
    private String userAgent;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected DeviceSessionJpaEntity() {}

    public DeviceSessionJpaEntity(UUID id, UUID userId, String deviceId, String deviceName, String platform,
            String ipAddress, String userAgent, OffsetDateTime revokedAt) {
        this.id = id;
        this.userId = userId;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.platform = platform;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.revokedAt = revokedAt;
    }

    public DeviceSessionJpaEntity(UUID userId, String deviceId, String deviceName, String platform, String ipAddress,
            String userAgent, OffsetDateTime revokedAt) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.platform = platform;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.revokedAt = revokedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    
    
}
