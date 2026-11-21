package com.sep.vox.domain.model.refreshtoken;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class RefreshToken {
    private UUID id;
    private UUID sessionId;
    private String tokenHash;
    private Instant issuedAt;
    private Instant expiredAt;
    private Instant usedAt;
    private UUID replacedBy;

    public RefreshToken() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
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

    public UUID getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(UUID replacedBy) {
        this.replacedBy = replacedBy;
    }

    private static final int DAY_TILL_EXPIRES = 7;

    public static RefreshToken createFresh(UUID sessionId, String tokenHash, Instant now) {
        return builder()
            .sessionId(sessionId)
            .tokenHash(tokenHash)
            .issuedAt(now)
            .expiredAt(now.plus(DAY_TILL_EXPIRES, ChronoUnit.DAYS))
            .build();
    }

    public boolean isUsed() {
        return this.usedAt != null;
    }

    public boolean isExpired(Instant now) {
        return this.expiredAt.isBefore(now);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RefreshToken target = new RefreshToken();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder sessionId(UUID sessionId) {
            target.sessionId = sessionId;
            return this;
        }

        public Builder tokenHash(String tokenHash) {
            target.tokenHash = tokenHash;
            return this;
        }

        public Builder issuedAt(Instant issuedAt) {
            target.issuedAt = issuedAt;
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

        public Builder replacedBy(UUID replacedBy) {
            target.replacedBy = replacedBy;
            return this;
        }

        public RefreshToken build() {
            return target;
        }
    }
}
