package com.sep.vox.domain.model.passwordsetuptoken;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PasswordSetUpToken {
    private static final int DAYS_UNTIL_EXPIRED = 2;

    private UUID id;
    private UUID userId;
    private String tokenHash;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiredAt;
    private OffsetDateTime usedAt;

    public PasswordSetUpToken() {}

    public PasswordSetUpToken(UUID id, UUID userId, String tokenHash, OffsetDateTime createdAt, OffsetDateTime expiredAt, OffsetDateTime usedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
        this.usedAt = usedAt;
    }

    public PasswordSetUpToken(UUID userId, String tokenHash, OffsetDateTime createdAt, OffsetDateTime expiredAt, OffsetDateTime usedAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
        this.usedAt = usedAt;
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

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public OffsetDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(OffsetDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public OffsetDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(OffsetDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static PasswordSetUpToken create(UUID userId, String tokenHash) {
        var now = OffsetDateTime.now();
        return new PasswordSetUpToken(
            userId, 
            tokenHash, 
            now, 
            now.plusDays(DAYS_UNTIL_EXPIRED), 
            null
        );
    }
}
