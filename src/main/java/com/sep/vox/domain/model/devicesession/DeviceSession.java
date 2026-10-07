package com.sep.vox.domain.model.devicesession;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.platform.Platform;
import com.sep.vox.domain.shared.BaseModel;

public class DeviceSession extends BaseModel {
    private UUID userId;
    private String deviceId;
    private String deviceName;
    private Platform platform;
    private String ipAddress;
    private String userAgent;
    private Instant revokedAt;

    public DeviceSession() {}

    public DeviceSession(UUID userId, String deviceId, String deviceName, Platform platform,
            String ipAddress, String userAgent, Instant revokedAt) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.platform = platform;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.revokedAt = revokedAt;
    }

    public DeviceSession(Builder builder) {
        super(builder.id, builder.createdAt);
        this.userId = builder.userId;
        this.deviceId = builder.deviceId;
        this.deviceName = builder.deviceName;
        this.platform = builder.platform;
        this.ipAddress = builder.ipAddress;
        this.userAgent = builder.userAgent;
        this.revokedAt = builder.revokedAt;
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

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
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

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public static Builder builder() {
        return new DeviceSession.Builder();
    }

    public static DeviceSession create(
        UUID userId, String deviceId, String deviceName, Platform platform, String ipAddress, String userAgent
    ) {
        return DeviceSession.builder()
            .userId(userId)
            .deviceId(deviceId)
            .deviceName(deviceName)
            .platform(platform)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();
    }
    
    public boolean isRevoked() {
        return this.revokedAt != null;
    }

    public boolean isDeviceIdMismatches(String deviceId) {
        return !this.deviceId.equals(deviceId);
    }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private String deviceId;
        private String deviceName;
        private Platform platform;
        private String ipAddress;
        private String userAgent;
        private Instant createdAt;
        private Instant revokedAt;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder deviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        public Builder platform(Platform platform) {
            this.platform = platform;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder revokedAt(Instant revokedAt) {
            this.revokedAt = revokedAt;
            return this;
        }

        public DeviceSession build() {
            return new DeviceSession(this);
        }
    }
}
