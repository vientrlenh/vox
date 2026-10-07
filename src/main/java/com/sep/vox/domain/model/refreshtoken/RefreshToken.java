package com.sep.vox.domain.model.refreshtoken;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.common.BaseModel;

public class RefreshToken extends BaseModel {
    private UUID deviceSessionId;
    private String tokenHash;
    private Instant expiresAt;
    private Instant usedAt;
    private UUID replacedBy;

    protected RefreshToken() {}

    public RefreshToken(UUID deviceSessionId, String tokenHash, Instant expiresAt,
            Instant usedAt, UUID replacedBy) {
        this.deviceSessionId = deviceSessionId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.replacedBy = replacedBy;
    }

    public RefreshToken(Builder builder) {
        super(builder.id, builder.createdAt);
        this.deviceSessionId = builder.deviceSessionId;
        this.tokenHash = builder.tokenHash;
        this.expiresAt = builder.expiresAt;
        this.usedAt = builder.usedAt;
        this.replacedBy = builder.replacedBy;
    }

    public UUID getDeviceSessionId() {
        return deviceSessionId;
    }

    public void setDeviceSessionId(UUID deviceSessionId) {
        this.deviceSessionId = deviceSessionId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public UUID getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(UUID replacedBy) {
        this.replacedBy = replacedBy;
    }

    public static Builder builder() {
        return new RefreshToken.Builder();
    }

    public static RefreshToken createFresh(UUID deviceSessionId, String tokenHash, Instant expiresAt) {
        return RefreshToken.builder()
            .deviceSessionId(deviceSessionId)
            .tokenHash(tokenHash)
            .expiresAt(expiresAt)
            .build();
    }

    public static RefreshToken replace(UUID deviceSessionId, String tokenHash, Instant expiresAt, Instant usedAt, UUID replacedBy) {
        return RefreshToken.builder()
            .deviceSessionId(deviceSessionId)
            .tokenHash(tokenHash)
            .expiresAt(expiresAt)
            .usedAt(usedAt)
            .replacedBy(replacedBy)
            .build();
    }

    public boolean isUsed() {
        return this.usedAt != null;
    }

    public boolean isExpired(Instant now) {
        return this.expiresAt.isBefore(now);
    }

    public static class Builder {
        private UUID id;
        private UUID deviceSessionId;
        private String tokenHash;
        private Instant createdAt;
        private Instant expiresAt;
        private Instant usedAt;
        private UUID replacedBy;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder deviceSessionId(UUID deviceSessionId) {
            this.deviceSessionId = deviceSessionId;
            return this;
        }

        public Builder tokenHash(String tokenHash) {
            this.tokenHash = tokenHash;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder usedAt(Instant usedAt) {
            this.usedAt = usedAt;
            return this;
        }

        public Builder replacedBy(UUID replacedBy) {
            this.replacedBy = replacedBy;
            return this;
        }

        public RefreshToken build() {
            return new RefreshToken(this);
        }
    }
}
