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

    public static NotificationDevice create(UUID userId, String deviceId, NotificationDevicePlatform platform, String installationId, Instant now) {
        return NotificationDevice.builder()
                .userId(userId)
                .deviceId(deviceId)
                .platform(platform)
                .installationId(installationId)
                .createdAt(now)
                .lastSeenAt(now)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final NotificationDevice target = new NotificationDevice();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder userId(UUID userId) {
            target.userId = userId;
            return this;
        }

        public Builder deviceId(String deviceId) {
            target.deviceId = deviceId;
            return this;
        }

        public Builder platform(NotificationDevicePlatform platform) {
            target.platform = platform;
            return this;
        }

        public Builder installationId(String installationId) {
            target.installationId = installationId;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public Builder lastSeenAt(Instant lastSeenAt) {
            target.lastSeenAt = lastSeenAt;
            return this;
        }

        public NotificationDevice build() {
            return target;
        }

    }
}
