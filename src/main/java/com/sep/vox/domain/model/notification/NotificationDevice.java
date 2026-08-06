package com.sep.vox.domain.model.notification;

import java.time.Instant;
import java.util.UUID;

public class NotificationDevice {
    private UUID id;
    private UUID userId;
    private String deviceId;
    private NotificationDevicePlatform platform;
    private String installationId;
    private Instant createdAt;
    private Instant lastSeenAt;

    public NotificationDevice() {}

    public NotificationDevice(UUID id, UUID userId, String deviceId, NotificationDevicePlatform platform, String installationId,
            Instant createdAt, Instant lastSeenAt) {
        this.id = id;
        this.userId = userId;
        this.deviceId = deviceId;
        this.platform = platform;
        this.installationId = installationId;
        this.createdAt = createdAt;
        this.lastSeenAt = lastSeenAt;
    }

    public NotificationDevice(UUID userId, String deviceId, NotificationDevicePlatform platform, String installationId,
            Instant createdAt, Instant lastSeenAt) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.platform = platform;
        this.installationId = installationId;
        this.createdAt = createdAt;
        this.lastSeenAt = lastSeenAt;
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

    public NotificationDevicePlatform getPlatform() {
        return platform;
    }

    public void setPlatform(NotificationDevicePlatform platform) {
        this.platform = platform;
    }

    public String getInstallationId() {
        return installationId;
    }

    public void setInstallationId(String installationId) {
        this.installationId = installationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    
}
