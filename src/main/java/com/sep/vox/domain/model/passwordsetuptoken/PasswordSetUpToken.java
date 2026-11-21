package com.sep.vox.domain.model.passwordsetuptoken;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class PasswordSetUpToken {
    private static final int DAYS_UNTIL_EXPIRED = 2;

    private UUID id;
    private UUID userId;
    private String tokenHash;
    private Instant createdAt;
    private Instant expiredAt;
    private Instant usedAt;

    public PasswordSetUpToken() {}

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

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Instant expiredAt) {
        this.expiredAt = expiredAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static PasswordSetUpToken create(UUID userId, String tokenHash) {
        Instant now = Instant.now();
        return builder()
            .userId(userId)
            .tokenHash(tokenHash)
            .createdAt(now)
            .expiredAt(now.plus(DAYS_UNTIL_EXPIRED, ChronoUnit.DAYS))
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final PasswordSetUpToken target = new PasswordSetUpToken();

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

        public Builder tokenHash(String tokenHash) {
            target.tokenHash = tokenHash;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public Builder expiredAt(Instant expiredAt) {
            target.expiredAt = expiredAt;
            return this;
        }

        public Builder usedAt(Instant usedAt) {
            target.usedAt = usedAt;
            return this;
        }

        public PasswordSetUpToken build() {
            return target;
        }
    }
}
