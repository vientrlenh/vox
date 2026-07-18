package com.sep.vox.domain.model.devicesession;

import java.time.OffsetDateTime;
import java.util.UUID;

public class DeviceSession {
    private UUID id;
    private UUID userId;
    private String deviceId;
    private String deviceName;
    private SessionPlatform platform;
    private String ipAddress;
    private String userAgent;
    private OffsetDateTime revokedAt;
    private String pushToken;

    public DeviceSession() {}

    public DeviceSession(UUID id, UUID userId, String deviceId, String deviceName, SessionPlatform platform,
            String ipAddress, String userAgent, OffsetDateTime revokedAt) {
        this(id, userId, deviceId, deviceName, platform, ipAddress, userAgent, revokedAt, null);
    }

    public DeviceSession(UUID userId, String deviceId, String deviceName, SessionPlatform platform, String ipAddress,
            String userAgent, OffsetDateTime revokedAt) {
        this(userId, deviceId, deviceName, platform, ipAddress, userAgent, revokedAt, null);
    }

    public DeviceSession(UUID id, UUID userId, String deviceId, String deviceName, SessionPlatform platform,
            String ipAddress, String userAgent, OffsetDateTime revokedAt, String pushToken) {
        this.id = id;
        this.userId = userId;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.platform = platform;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.revokedAt = revokedAt;
        this.pushToken = pushToken;
    }

    public DeviceSession(UUID userId, String deviceId, String deviceName, SessionPlatform platform, String ipAddress,
            String userAgent, OffsetDateTime revokedAt, String pushToken) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.platform = platform;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.revokedAt = revokedAt;
        this.pushToken = pushToken;
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

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getPushToken() {
        return pushToken;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }


    public static DeviceSession create(UUID userId, String deviceId, String deviceName, SessionPlatform platform, String ipAddress, String userAgent) {
        return new DeviceSession(
            userId,
            deviceId,
            deviceName,
            platform,
            ipAddress,
            userAgent,
            null,
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
