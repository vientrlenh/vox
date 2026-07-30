package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(columnList = "session_id", name = "idx_refresh_token_sessions"),
    @Index(columnList = "token_hash", name = "idx_refresh_token_hash", unique = true)
})
public class RefreshTokenJpaEntity {
    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false, 
        insertable = false, 
        columnDefinition = "UUID DEFAULT uuidv7()")
    private UUID id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 512)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expired_at", nullable = false, updatable = false)
    private Instant expiredAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;


    protected RefreshTokenJpaEntity() {}

    

    public RefreshTokenJpaEntity(UUID id, UUID sessionId, String tokenHash, Instant issuedAt,
            Instant expiredAt, Instant usedAt, UUID replacedBy) {
        this.id = id;
        this.sessionId = sessionId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiredAt = expiredAt;
        this.usedAt = usedAt;
        this.replacedBy = replacedBy;
    }


    public RefreshTokenJpaEntity(UUID sessionId, String tokenHash, Instant issuedAt, Instant expiredAt,
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

    
}
