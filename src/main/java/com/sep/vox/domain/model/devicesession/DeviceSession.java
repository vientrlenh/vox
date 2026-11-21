package com.sep.vox.domain.model.devicesession;

import java.time.Instant;
import java.util.UUID;

public class DeviceSession {
    private UUID id;
    private UUID userId;
    private String deviceId;
    private String deviceName;
    private SessionPlatform platform;
    private String ipAddress;
    private String userAgent;
    private Instant revokedAt;

    public DeviceSession() {}

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

    public SessionPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(SessionPlatform platform) {
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

    
    public static DeviceSession create(UUID userId, String deviceId, String deviceName, SessionPlatform platform, String ipAddress, String userAgent) {
        return builder()
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DeviceSession target = new DeviceSession();

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

        public Builder deviceName(String deviceName) {
            target.deviceName = deviceName;
            return this;
        }

        public Builder platform(SessionPlatform platform) {
            target.platform = platform;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            target.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            target.userAgent = userAgent;
            return this;
        }

        public Builder revokedAt(Instant revokedAt) {
            target.revokedAt = revokedAt;
            return this;
        }

        public DeviceSession build() {
            return target;
        }
    }
}
