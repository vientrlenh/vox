package com.sep.vox.domain.model.session;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Session {
    private UUID id;
    private UUID userId;
    private String refreshTokenHash;
    private OffsetDateTime issuedAt;
    private OffsetDateTime expiredAt;
    private OffsetDateTime revokedAt;
    private UUID replacedBy;

    public Session() {}

    public Session(UUID id, UUID userId, String refreshTokenHash, OffsetDateTime issuedAt, OffsetDateTime expiredAt,
            OffsetDateTime revokedAt, UUID replacedBy) {
        this.id = id;
        this.userId = userId;
        this.refreshTokenHash = refreshTokenHash;
        this.issuedAt = issuedAt;
        this.expiredAt = expiredAt;
        this.revokedAt = revokedAt;
        this.replacedBy = replacedBy;
    }

    public Session(UUID userId, String refreshTokenHash, OffsetDateTime issuedAt, OffsetDateTime expiredAt,
            OffsetDateTime revokedAt, UUID replacedBy) {
        this.userId = userId;
        this.refreshTokenHash = refreshTokenHash;
        this.issuedAt = issuedAt;
        this.expiredAt = expiredAt;
        this.revokedAt = revokedAt;
        this.replacedBy = replacedBy;
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

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public void setRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
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

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public UUID getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(UUID replacedBy) {
        this.replacedBy = replacedBy;
    }

    
}
