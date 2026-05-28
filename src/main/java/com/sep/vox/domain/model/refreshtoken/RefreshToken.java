package com.sep.vox.domain.model.refreshtoken;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RefreshToken {
    private UUID id;
    private UUID sessionId;
    private String tokenHash;
    private OffsetDateTime issuedAt;
    private OffsetDateTime expiredAt;
    private OffsetDateTime usedAt;
    private UUID replacedBy;

    public RefreshToken() {}

    public RefreshToken(UUID id, UUID sessionId, String tokenHash, OffsetDateTime issuedAt, OffsetDateTime expiredAt,
            OffsetDateTime usedAt, UUID replacedBy) {
        this.id = id;
        this.sessionId = sessionId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiredAt = expiredAt;
        this.usedAt = usedAt;
        this.replacedBy = replacedBy;
    }

    public RefreshToken(UUID sessionId, String tokenHash, OffsetDateTime issuedAt, OffsetDateTime expiredAt,
            OffsetDateTime usedAt, UUID replacedBy) {
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

    public OffsetDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(OffsetDateTime issuedAt) {
        this.issuedAt = issuedAt;
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

    public UUID getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(UUID replacedBy) {
        this.replacedBy = replacedBy;
    }

    private static final int DAY_TILL_EXPIRES = 7;

    public static RefreshToken createFresh(UUID sessionId, String tokenHash, OffsetDateTime now) {
        return new RefreshToken(
            sessionId, 
            tokenHash, 
            now,
            now.plusDays(DAY_TILL_EXPIRES), 
            null, 
            null
        );
    }
}
