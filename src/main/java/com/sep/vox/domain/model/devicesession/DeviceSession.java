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

    public DeviceSession(UUID id, UUID userId, String deviceId, String deviceName, SessionPlatform platform,
            String ipAddress, String userAgent, Instant revokedAt) {
        this.id = id;
        this.userId = userId;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.platform = platform;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.revokedAt = revokedAt;
    }

    public DeviceSession(UUID userId, String deviceId, String deviceName, SessionPlatform platform, String ipAddress,
            String userAgent, Instant revokedAt) {
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
        return new DeviceSession(
            userId, 
            deviceId, 
            deviceName, 
            platform, 
            ipAddress, 
            userAgent, 
            null
        );
    }
    
    public boolean isRevoked() {
        return this.revokedAt != null;
    }

    public boolean isDeviceIdMismatches(String deviceId) {
        return !this.deviceId.equals(deviceId);
    }
}
