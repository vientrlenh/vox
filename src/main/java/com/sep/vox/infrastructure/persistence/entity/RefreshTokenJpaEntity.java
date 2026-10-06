package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.infrastructure.shared.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(columnList = "device_session_id", name = "idx_refresh_token_device_sessions"),
    @Index(columnList = "token_hash", name = "idx_refresh_token_token_hash", unique = true)
})
@Getter 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder 
public class RefreshTokenJpaEntity extends BaseEntity {

    @Column(name = "device_session_id", nullable = false, updatable = false)
    private UUID deviceSessionId;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 512)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;

}
