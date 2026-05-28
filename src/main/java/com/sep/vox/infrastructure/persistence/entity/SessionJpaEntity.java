package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sessions")
public class SessionJpaEntity {
    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        updatable = false,
        nullable = false,
        insertable = false,
        columnDefinition = "UUID default uuidv7()"
    )
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "refresh_token_hash", nullable = false, updatable = false)
    private String refreshTokenHash;

    @Column(name = "issued_at", updatable = false, nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expired_at", updatable = false, nullable = false)
    private OffsetDateTime expiredAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    protected SessionJpaEntity() {}

    public SessionJpaEntity(UUID id, UUID userId, String refreshTokenHash, OffsetDateTime issuedAt,
            OffsetDateTime expiredAt, OffsetDateTime revokedAt, UUID replacedBy) {
        this.id = id;
        this.userId = userId;
        this.refreshTokenHash = refreshTokenHash;
        this.issuedAt = issuedAt;
        this.expiredAt = expiredAt;
        this.revokedAt = revokedAt;
        this.replacedBy = replacedBy;
    }

    public SessionJpaEntity(UUID userId, String refreshTokenHash, OffsetDateTime issuedAt, OffsetDateTime expiredAt,
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
