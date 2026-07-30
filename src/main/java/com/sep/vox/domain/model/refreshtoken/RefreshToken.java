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

    public RefreshToken(UUID id, UUID sessionId, String tokenHash, Instant issuedAt, Instant expiredAt,
            Instant usedAt, UUID replacedBy) {
        this.id = id;
        this.sessionId = sessionId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiredAt = expiredAt;
        this.usedAt = usedAt;
        this.replacedBy = replacedBy;
    }

    public RefreshToken(UUID sessionId, String tokenHash, Instant issuedAt, Instant expiredAt,
            Instant usedAt, UUID replacedBy) {
        this.sessionId = sessionId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiredAt = expiredAt;
        this.usedAt = usedAt;
        this.replacedBy = replacedBy;
    }

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
        return new RefreshToken(
            sessionId, 
            tokenHash, 
            now,
            now.plus(DAY_TILL_EXPIRES, ChronoUnit.DAYS), 
            null, 
            null
        );
    }

    public boolean isUsed() {
        return this.usedAt != null;
    }

    public boolean isExpired(Instant now) {
        return this.expiredAt.isBefore(now);
    }
}
