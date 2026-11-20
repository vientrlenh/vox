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

    public PasswordSetUpToken(UUID id, UUID userId, String tokenHash, Instant createdAt, Instant expiredAt, Instant usedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
        this.usedAt = usedAt;
    }

    public PasswordSetUpToken(UUID userId, String tokenHash, Instant createdAt, Instant expiredAt, Instant usedAt) {
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
        var now = Instant.now();
        return new PasswordSetUpToken(
            userId, 
            tokenHash, 
            now, 
            now.plus(DAYS_UNTIL_EXPIRED, ChronoUnit.DAYS), 
            null
        );
    }
}
